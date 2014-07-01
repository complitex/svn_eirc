package ru.flexpay.eirc.registry.service.link;

import org.apache.commons.lang.StringUtils;
import org.complitex.correction.service.AddressService;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.EircConfig;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.handle.AbstractMessenger;
import ru.flexpay.eirc.registry.service.parse.RegistryRecordWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.TransitionNotAllowed;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryLinker {

    private static final Logger log = LoggerFactory.getLogger(RegistryLinker.class);

    @EJB
    private ConfigBean configBean;

    @EJB
    private JobProcessor processor;

    @EJB
    private LinkQueueProcessor linkQueueProcessor;

    @EJB
    private RegistryBean registryBean;

    @EJB
    private RegistryWorkflowManager registryWorkflowManager;

    @EJB
    private RegistryRecordBean registryRecordBean;

    @EJB
    private RegistryRecordWorkflowManager registryRecordWorkflowManager;

    @EJB
    private AddressService addressService;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private ModuleInstanceStrategy moduleInstanceStrategy;

    private static final ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock();

    public void link(final Long registryId, final AbstractMessenger imessenger, final AbstractFinishCallback finishLink) {
        link(FilterWrapper.<RegistryRecordData>of(new RegistryRecord(registryId)), imessenger, finishLink, false);
    }

    public void linkAfterCorrection(RegistryRecordData record, final AbstractMessenger imessenger, final AbstractFinishCallback finishLink) {
        link(FilterWrapper.<RegistryRecordData>of(record), imessenger, finishLink, true);
    }

    private void link(final FilterWrapper<RegistryRecordData> filter, final AbstractMessenger imessenger,
                      final AbstractFinishCallback finishLink, final boolean afterCorrection) {
        final AtomicBoolean finishReadRecords = new AtomicBoolean(false);
        final AtomicInteger recordLinkingCounter = new AtomicInteger(0);

        final Long registryId = filter.getObject().getRegistryId();

        final FilterWrapper<Registry> registryFilter = FilterWrapper.of(new Registry(registryId));

        final AtomicLong userOrganizationId = new AtomicLong(0);

        try {
            Long moduleId = configBean.getInteger(EircConfig.MODULE_ID, true).longValue();
            DomainObject module = moduleInstanceStrategy.findById(moduleId, true);
            userOrganizationId.set(AttributeUtil.getIntegerValue(module, ModuleInstanceStrategy.ORGANIZATION).longValue());
        } catch (Exception e) {
            imessenger.addMessageError("eirc_organization_id_not_defined");
            log.error("Can not get {} from config: {}", EircConfig.MODULE_ID, e.toString());
            return;
        }

        imessenger.addMessageInfo("starting_link_registries", registryId);
        finishLink.init();

        linkQueueProcessor.execute(new AbstractJob<Void>() {
            @Override
            public Void execute() throws ExecuteException {
                try {

                    List<Registry> registries = registryBean.getRegistries(registryFilter);

                    // check registry exist
                    if (registries.size() == 0) {
                        imessenger.addMessageInfo("registry_not_found", registryId);
                        return null;
                    }

                    final Registry registry = registries.get(0);
                    registryFilter.setObject(registry);


                    // one process on linking
                    registryLock.writeLock().lock();
                    try {
                        // check registry status
                        if (!registryWorkflowManager.canLink(registry)) {
                            imessenger.addMessageError("registry_failed_status", registry.getRegistryNumber());
                            return null;
                        }

                        // change registry status
                        if (!EjbBeanLocator.getBean(RegistryLinker.class).setLinkingStatus(registry)) {
                            imessenger.addMessageError("registry_status_inner_error", registry.getRegistryNumber());
                            return null;
                        }
                    } finally {
                        registryLock.writeLock().unlock();
                    }

                    // check registry records status
                    if (!registryRecordBean.hasRecordsToLinking(registry)) {
                        imessenger.addMessageInfo("not_found_linking_registry_records", registry.getRegistryNumber());
                        EjbBeanLocator.getBean(RegistryLinker.class).setLinkingStatus(registry);
                        return null;
                    }

                    try {

                        final BatchProcessor<JobResult> batchProcessor = new BatchProcessor<>(10, processor);

                        final Statistics statistics = new Statistics(registry.getRegistryNumber(), imessenger);

                        int numberFlushRegistryRecords = configBean.getInteger(EircConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                        List<RegistryRecordData> registryRecords;
                        FilterWrapper<RegistryRecordData> innerFilter = FilterWrapper.of(filter.getObject(), 0, numberFlushRegistryRecords);
                        do {
                            final List<RegistryRecordData> recordsToLinking = afterCorrection?
                                    registryRecordBean.getCorrectionRecordsToLinking(innerFilter) :
                                    registryRecordBean.getRecordsToLinking(innerFilter);

                            if (recordsToLinking.size() < numberFlushRegistryRecords) {
                                finishReadRecords.set(true);
                            }

                            if (recordsToLinking.size() > 0) {

                                recordLinkingCounter.incrementAndGet();

                                batchProcessor.processJob(new AbstractJob<JobResult>() {
                                    @Override
                                    public JobResult execute() throws ExecuteException {

                                        try {
                                            int successLinked = EjbBeanLocator.getBean(RegistryLinker.class).linkRegistryRecords(registry, recordsToLinking, userOrganizationId.get());

                                            statistics.add(recordsToLinking.size(), successLinked, recordsToLinking.size() - successLinked);

                                            return JobResult.SUCCESSFUL;
                                        } catch (Throwable th) {
                                            EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);
                                            imessenger.addMessageError("registry_failed_linked", registry.getRegistryNumber());
                                            throw new ExecuteException(th, "Failed upload registry " + registryId);
                                        } finally {
                                            if (recordLinkingCounter.decrementAndGet() == 0 && finishReadRecords.get()) {
                                                imessenger.addMessageInfo("registry_finish_link", registry.getRegistryNumber());
                                                finishLink.complete();
                                                if (afterCorrection &&
                                                        registryRecordBean.getRecordsToLinking(FilterWrapper.of(filter.getObject(), 0, 1)).size() > 0) {
                                                    EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);
                                                }
                                                EjbBeanLocator.getBean(RegistryLinker.class).setLinkedStatus(registry);
                                            }
                                        }
                                    }
                                });

                                // next registry record`s id is last in this partition
                                innerFilter.setFirst(recordsToLinking.get(recordsToLinking.size() - 1).getId().intValue() + 1);
                            } else if (recordLinkingCounter.get() == 0) {
                                imessenger.addMessageInfo("registry_finish_link", registry.getRegistryNumber());
                                finishLink.complete();
                                if (afterCorrection &&
                                        registryRecordBean.getRecordsToLinking(FilterWrapper.of(filter.getObject(), 0, 1)).size() > 0) {
                                    EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);
                                }
                                EjbBeanLocator.getBean(RegistryLinker.class).setLinkedStatus(registry);
                            }
                            registryRecords = recordsToLinking;
                        } while (registryRecords.size() >= numberFlushRegistryRecords);

                    } catch (Throwable th) {

                        log.error("Can not link registry " + registryId, th);

                        EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);

                    }
                } finally {
                    if (!finishReadRecords.get()) {
                        imessenger.addMessageInfo("registry_finish_link", registryFilter.getObject().getRegistryNumber() != null?
                                registryFilter.getObject().getRegistryNumber() : registryFilter.getObject().getId());
                        finishLink.complete();
                    }
                }
                return null;
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int linkRegistryRecords(Registry registry, List<RegistryRecordData> registryRecords, Long userOrganizationId) throws TransitionNotAllowed {
        int successLinked = 0;
        for (RegistryRecordData registryRecord : registryRecords) {

            // Search address
            if (registryRecord.getStatus() == RegistryRecordStatus.LOADED ||
                    registryRecord.getImportErrorType() != null &&
                            (registryRecord.getImportErrorType().getId() < 17 || registryRecord.getImportErrorType().getId() > 18)) {
                addressService.resolveAddress(userOrganizationId, registry.getSenderOrganizationId(), registryRecord);
                if (registryRecord.getImportErrorType() != null) {
                    registryWorkflowManager.markLinkingHasError(registry);
                    continue;
                }
            }

            // Search service provider account
            Address address = registryRecord.getAddress();

            EircAccount eircAccount = new EircAccount();
            eircAccount.setAddress(address);

            ServiceProviderAccount account = new ServiceProviderAccount(eircAccount);
            account.setOrganizationId(registry.getRecipientOrganizationId());

            Service service = new Service();
            service.setCode(registryRecord.getServiceCode());

            account.setService(service);

            FilterWrapper<ServiceProviderAccount> filter = FilterWrapper.of(account);
            filter.setSortProperty(null);

            List<ServiceProviderAccount> accounts =
                    serviceProviderAccountBean.getServiceProviderAccounts(filter);

            if (accounts.size() > 1) {
                registryRecordWorkflowManager.setNextErrorStatus(registryRecord, registry, ImportErrorType.MORE_ONE_ACCOUNT);
            } else if (accounts.size() == 1 &&
                    !StringUtils.equals(accounts.get(0).getAccountNumber(), registryRecord.getPersonalAccountExt())) {
                registryRecordWorkflowManager.setNextErrorStatus(registryRecord, registry, ImportErrorType.ACCOUNT_UNRESOLVED);
            } else {
                // success linked
                registryRecordWorkflowManager.setNextSuccessStatus(registryRecord);
                successLinked++;
            }

        }

        registryRecordBean.updateBulk(registryRecords);

        return successLinked;

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setLinkingStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set linking status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setLinkedStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can set set linked status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setErrorStatus(Registry registry) {
        try {
            registryWorkflowManager.markLinkingHasError(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set error status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    private class Statistics {
        private int totalLinkedRecords = 0;
        private int successLinkedRecords = 0;
        private int errorLinkedRecords = 0;

        private Lock lock = new ReentrantLock();

        private Long registryNumber;
        private AbstractMessenger imessenger;

        private Statistics(Long registryNumber, AbstractMessenger imessenger) {
            this.registryNumber = registryNumber;
            this.imessenger = imessenger;
        }

        public int getTotalLinkedRecords() {
            return totalLinkedRecords;
        }

        public int getSuccessLinkedRecords() {
            return successLinkedRecords;
        }

        public int getErrorLinkedRecords() {
            return errorLinkedRecords;
        }

        public void add(int totalLinkedRecords, int successLinkedRecords, int errorLinkedRecords) {
            lock.lock();
            try {
                this.totalLinkedRecords   += totalLinkedRecords;
                this.successLinkedRecords += successLinkedRecords;
                this.errorLinkedRecords   += errorLinkedRecords;

                imessenger.addMessageInfo("linked_bulk_records", this.totalLinkedRecords, this.successLinkedRecords,
                        this.errorLinkedRecords, registryNumber);
            } finally {
                lock.unlock();
            }
        }
    }

}
