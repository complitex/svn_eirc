package ru.flexpay.eirc.registry.service.link;

import org.apache.commons.lang.StringUtils;
import org.complitex.address.entity.AddressEntity;
import org.complitex.correction.service.AddressService;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.parse.RegistryRecordWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.TransitionNotAllowed;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Singleton
public class RegistryLinker {

    private final Logger log = LoggerFactory.getLogger(RegistryLinker.class);

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

    public void link(final Long registryId, final IMessenger imessenger, final FinishCallback finishLink) {
        link(FilterWrapper.of(new RegistryRecord(registryId)), imessenger, finishLink, false);
    }

    public void linkAfterCorrection(RegistryRecord record, final IMessenger imessenger, final FinishCallback finishLink) {
        link(FilterWrapper.of(record), imessenger, finishLink, true);
    }

    private void link(final FilterWrapper<RegistryRecord> filter, final IMessenger imessenger,
                      final FinishCallback finishLink, final boolean afterCorrection) {
        final AtomicBoolean finishReadRecords = new AtomicBoolean(false);
        final AtomicInteger recordLinkingCounter = new AtomicInteger(0);

        final Long registryId = filter.getObject().getRegistryId();

        final FilterWrapper<Registry> registryFilter = FilterWrapper.of(new Registry(registryId));

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
                    synchronized (this) {
                        // check registry status
                        if (!registryWorkflowManager.canLink(registry)) {
                            imessenger.addMessageError("registry_failed_status", registry.getRegistryNumber());
                            return null;
                        }

                        // change registry status
                        if (!setLinkingStatus(registry)) {
                            imessenger.addMessageError("registry_status_inner_error", registry.getRegistryNumber());
                            return null;
                        }
                    }

                    // check registry records status
                    if (!registryRecordBean.hasRecordsToLinking(registry)) {
                        imessenger.addMessageInfo("not_found_linking_registry_records", registry.getRegistryNumber());
                        setLinkingStatus(registry);
                        return null;
                    }

                    try {

                        final BatchProcessor<JobResult> batchProcessor = new BatchProcessor<>(10, processor);

                        int numberFlushRegistryRecords = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                        List<RegistryRecord> registryRecords;
                        FilterWrapper<RegistryRecord> innerFilter = FilterWrapper.of(filter.getObject(), 0, numberFlushRegistryRecords);
                        do {
                            final List<RegistryRecord> recordsToLinking = afterCorrection?
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
                                            linkRegistryRecords(registry, recordsToLinking);

                                            return JobResult.SUCCESSFUL;
                                        } catch (Throwable th) {
                                            setErrorStatus(registry);
                                            imessenger.addMessageError("registry_failed_linked", registry.getRegistryNumber());
                                            throw new ExecuteException(th, "Failed upload registry " + registryId);
                                        } finally {
                                            if (recordLinkingCounter.decrementAndGet() == 0 && finishReadRecords.get()) {
                                                imessenger.addMessageInfo("registry_finish_link", registry.getRegistryNumber());
                                                finishLink.complete();
                                                setLinkedStatus(registry);
                                            }
                                        }
                                    }
                                });

                                // next registry record`s id is last in this partition
                                innerFilter.setFirst(recordsToLinking.get(recordsToLinking.size() - 1).getId().intValue() + 1);
                            } else if (recordLinkingCounter.get() == 0) {
                                imessenger.addMessageInfo("registry_finish_link", registry.getRegistryNumber());
                                finishLink.complete();
                                setLinkedStatus(registry);
                            }
                            registryRecords = recordsToLinking;
                        } while (registryRecords.size() >= numberFlushRegistryRecords);

                    } catch (Throwable th) {

                        log.error("Can not link registry " + registryId, th);

                        setErrorStatus(registry);

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
    public void linkRegistryRecords(Registry registry, List<RegistryRecord> registryRecords) throws TransitionNotAllowed {
        for (RegistryRecord registryRecord : registryRecords) {

            // Search address
            if (registryRecord.getStatus() == RegistryRecordStatus.LOADED ||
                    registryRecord.getImportErrorType() != null && registryRecord.getImportErrorType().getId() < 17) {
                addressService.resolveAddress(registry.getRecipientOrganizationId(), registry.getSenderOrganizationId(), registryRecord);
                if (registryRecord.getImportErrorType() != null) {
                    registryWorkflowManager.markLinkingHasError(registry);
                    continue;
                }
            }

            // Search service provider account
            Address address = null;
            if (registryRecord.getApartmentId() != null) {
                address = new Address(registryRecord.getApartmentId(), AddressEntity.APARTMENT);
            } else if (registryRecord.getBuildingId() != null) {
                address = new Address(registryRecord.getBuildingId(), AddressEntity.BUILDING);
            }

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
            }

        }

        registryRecordBean.updateBulk(registryRecords);

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

}
