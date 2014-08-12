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
import org.slf4j.cal10n.LocLogger;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.EircConfig;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.entity.log.GeneralProcessing;
import ru.flexpay.eirc.registry.entity.log.Linking;
import ru.flexpay.eirc.registry.service.*;
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

    @EJB
    private CanceledProcessing canceledProcessing;

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
            final LocLogger logger = getProcessLogger(null, imessenger);
            logger.error(GeneralProcessing.EIRC_ORGANIZATION_ID_NOT_DEFINED);
            return;
        }

        List<Registry> registries = registryBean.getRegistries(registryFilter);

        // check registry exist
        if (registries.size() == 0) {
            final LocLogger logger = getProcessLogger(registryId, imessenger);
            logger.info(GeneralProcessing.REGISTRY_NOT_FOUND);
            return;
        }

        final Registry registry = registries.get(0);
        registryFilter.setObject(registry);

        final LocLogger logger = getProcessLogger(registry.getRegistryNumber(), imessenger);
        logger.info(Linking.STARTING_LINK_REGISTRIES);
        finishLink.init(registry.getId());

        linkQueueProcessor.execute(new AbstractJob<Void>() {
            @Override
            public Void execute() throws ExecuteException {
                try {

                    // one process on linking
                    registryLock.writeLock().lock();
                    try {
                        // check registry status
                        if (!registryWorkflowManager.canLink(registry)) {
                            logger.error(Linking.REGISTRY_FAILED_STATUS);
                            return null;
                        }

                        // change registry status
                        if (!EjbBeanLocator.getBean(RegistryLinker.class).setLinkingStatus(registry)) {
                            logger.error(Linking.REGISTRY_STATUS_INNER_ERROR);
                            return null;
                        }
                    } finally {
                        registryLock.writeLock().unlock();
                    }

                    // check registry records status
                    if (!registryRecordBean.hasRecordsToLinking(registry)) {
                        logger.info(Linking.NOT_FOUND_LINKING_REGISTRY_RECORDS);
                        EjbBeanLocator.getBean(RegistryLinker.class).setLinkingStatus(registry);
                        return null;
                    }

                    try {

                        final BatchProcessor<JobResult> batchProcessor = new BatchProcessor<>(10, processor);

                        final Statistics statistics = new Statistics(registry.getRegistryNumber(), imessenger);

                        int numberFlushRegistryRecords = configBean.getInteger(EircConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                        FilterWrapper<RegistryRecordData> innerFilter = FilterWrapper.of(filter.getObject(), 0, numberFlushRegistryRecords);
                        do {
                            final List<RegistryRecordData> recordsToLinking = afterCorrection?
                                    registryRecordBean.getCorrectionRecordsToLinking(innerFilter) :
                                    registryRecordBean.getRecordsToLinking(innerFilter);

                            if (!isContinue(recordsToLinking, registry)) {
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
                                            logger.error(Linking.REGISTRY_FAILED_LINKED, th.getMessage());
                                            throw new ExecuteException(th, "Failed link registry " + registryId);
                                        } finally {
                                            if (recordLinkingCounter.decrementAndGet() == 0 && finishReadRecords.get()) {
                                                finalizeRegistryLinked(logger, finishLink, registry, afterCorrection, filter);
                                            }
                                        }
                                    }
                                });

                                // next registry record`s id is last in this partition
                                innerFilter.setFirst(recordsToLinking.get(recordsToLinking.size() - 1).getId().intValue() + 1);
                            } else if (recordLinkingCounter.get() == 0) {
                                finalizeRegistryLinked(logger, finishLink, registry, afterCorrection, filter);
                            }
                        } while (!finishReadRecords.get());

                    } catch (Throwable th) {

                        logger.error(Linking.REGISTRY_FAILED_LINKED, th.getMessage());
                        log.error("Can not link registry " + registryId, th);

                        EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);

                    }
                } finally {
                    if (!finishReadRecords.get()) {
                        if (registryWorkflowManager.isLinking(registry)) {
                            try {
                                setErrorStatus(registry);
                            } catch (Throwable th) {
                                log.error("Can not change status", th);
                                logger.error(Linking.REGISTRY_STATUS_INNER_ERROR);
                            }
                            try {
                                setLinkedStatus(registry);
                            } catch (Throwable th) {
                                log.error("Can not change status", th);
                                logger.error(Linking.REGISTRY_STATUS_INNER_ERROR);
                            }
                        }
                        logger.info(Linking.REGISTRY_FINISH_LINK);
                        finishLink.complete();
                    }
                }
                return null;
            }
        });
    }

    public void finalizeRegistryLinked(final LocLogger logger, AbstractFinishCallback finishLink, final Registry registry,
                                       boolean afterCorrection, FilterWrapper<RegistryRecordData> filter) throws ExecuteException {

        finishLink.complete();

        // Проставляем статус отмены
        if (canceledProcessing.isCancel(registry.getId(), new Runnable() {
            @Override
            public void run() {
                EjbBeanLocator.getBean(RegistryLinker.class).setCancelStatus(registry);
                logger.error(Linking.LINKING_CANCELED);
            }

        })) {
            return;
        }

        logger.info(Linking.REGISTRY_FINISH_LINK);

        // если не было отмены, то статус завершения
        if (afterCorrection &&
                registryRecordBean.getRecordsToLinking(FilterWrapper.of(filter.getObject(), 0, 1)).size() > 0) {
            EjbBeanLocator.getBean(RegistryLinker.class).setErrorStatus(registry);
        }
        EjbBeanLocator.getBean(RegistryLinker.class).setLinkedStatus(registry);

    }

    protected boolean isContinue(final List<RegistryRecordData> data, final Registry registry) throws ExecuteException {
        return data.size() != 0 && !canceledProcessing.isCanceling(registry.getId());
        /*
        canceledProcessing.isCancel(registry.getId(), new Runnable() {
            @Override
            public void run() {
                finishReadRecords.set(true);
                finishCallback.waitCompleted();
                EjbBeanLocator.getBean(RegistryLinker.class).setCancelStatus(registry);
                logger.error(Linking.LINKING_CANCELED);
            }
        });
        */
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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setCancelStatus(Registry registry) {
        try {
            registryWorkflowManager.markLinkingCanceled(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set linking canceled status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    private LocLogger getProcessLogger(Long registryId, AbstractMessenger imessenger) {
        return RegistryLogger.getInstance(registryId, imessenger, RegistryLinker.class);
    }

    private class Statistics {
        private int totalLinkedRecords = 0;
        private int successLinkedRecords = 0;
        private int errorLinkedRecords = 0;

        private Lock lock = new ReentrantLock();
        private LocLogger logger;

        private Statistics(Long registryNumber, AbstractMessenger imessenger) {
            this.logger = getProcessLogger(registryNumber, imessenger);
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

                logger.info(Linking.LINKED_BULK_RECORDS, this.totalLinkedRecords, this.successLinkedRecords, this.errorLinkedRecords);
            } finally {
                lock.unlock();
            }
        }
    }

}
