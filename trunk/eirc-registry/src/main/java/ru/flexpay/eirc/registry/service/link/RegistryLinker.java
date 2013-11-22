package ru.flexpay.eirc.registry.service.link;

import org.complitex.correction.service.AddressService;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryConfig;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.registry.entity.RegistryRecordStatus;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.parse.RegistryRecordWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.TransitionNotAllowed;

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

    public void link(final Long registryId, final IMessenger imessenger, final FinishCallback finishLink) {
        final AtomicBoolean finishReadRecords = new AtomicBoolean(false);
        final AtomicInteger recordLinkingCounter = new AtomicInteger(0);

        linkQueueProcessor.execute(new AbstractJob<Void>() {
            @Override
            public Void execute() throws ExecuteException {
                try {
                    imessenger.addMessageInfo("starting_link_registries", registryId);
                    finishLink.init();

                    List<Registry> registries = registryBean.getRegistries(FilterWrapper.of(new Registry(registryId)));

                    // check registry exist
                    if (registries.size() == 0) {
                        imessenger.addMessageInfo("registry_not_found", registryId);
                        return null;
                    }

                    final Registry registry = registries.get(0);

                    // one process on linking
                    synchronized (this) {
                        // check registry status
                        if (!registryWorkflowManager.canLink(registry)) {
                            imessenger.addMessageError("registry_failed_status", registryId);
                            return null;
                        }

                        // change registry status
                        if (!setLinkingStatus(registry)) {
                            imessenger.addMessageError("registry_status_inner_error", registryId);
                            return null;
                        }
                    }

                    // check registry records status
                    if (!registryRecordBean.hasRecordsToLinking(registry)) {
                        imessenger.addMessageInfo("not_found_linking_registry_records", registryId);
                        setLinkingStatus(registry);
                        return null;
                    }

                    try {

                        final BatchProcessor<JobResult> batchProcessor = new BatchProcessor<>(10, processor);

                        int numberFlushRegistryRecords = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                        List<RegistryRecord> registryRecords;
                        FilterWrapper<RegistryRecord> filter = FilterWrapper.of(new RegistryRecord(registryId), 0, numberFlushRegistryRecords);
                        do {
                            final List<RegistryRecord> recordsToLinking = registryRecordBean.getRecordsToLinking(filter);

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
                                            imessenger.addMessageError("registry_failed_linked", registryId);
                                            throw new ExecuteException(th, "Failed upload registry " + registryId);
                                        } finally {
                                            if (recordLinkingCounter.decrementAndGet() == 0 && finishReadRecords.get()) {
                                                imessenger.addMessageInfo("registry_finish_link", registryId);
                                                finishLink.complete();
                                                setLinkedStatus(registry);
                                            }
                                        }
                                    }
                                });

                                // next registry record`s id is last in this partition
                                filter.setFirst(recordsToLinking.get(recordsToLinking.size() - 1).getId().intValue() + 1);
                            } else if (recordLinkingCounter.get() == 0) {
                                imessenger.addMessageInfo("registry_finish_link", registryId);
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
                        imessenger.addMessageInfo("registry_finish_link", registryId);
                        finishLink.complete();
                    }
                }
                return null;
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void linkRegistryRecords(Registry registry, List<RegistryRecord> registryRecords) throws TransitionNotAllowed {
        for (RegistryRecord registryRecord : registryRecords) {
            addressService.resolveAddress(registry.getRecipientOrganizationId(), registry.getSenderOrganizationId(), registryRecord);
            if (registryRecord.getStatus() != RegistryRecordStatus.LINKED) {
                registryWorkflowManager.markLinkingHasError(registry);
            }
        }

        registryRecordBean.saveBulk(registryRecords);

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private boolean setLinkingStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set linking status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private boolean setLinkedStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can set set linked status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private boolean setErrorStatus(Registry registry) {
        try {
            registryWorkflowManager.markLinkingHasError(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set error status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

}
