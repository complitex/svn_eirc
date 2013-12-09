package ru.flexpay.eirc.registry.service.handle;

import com.google.common.collect.Lists;
import org.complitex.correction.service.AddressService;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryConfig;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.handle.exchange.Operation;
import ru.flexpay.eirc.registry.service.handle.exchange.OperationFactory;
import ru.flexpay.eirc.registry.service.handle.exchange.OperationResult;
import ru.flexpay.eirc.registry.service.parse.RegistryRecordWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;
import ru.flexpay.eirc.registry.service.parse.TransitionNotAllowed;
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
public class RegistryHandler {

    private static final Logger log = LoggerFactory.getLogger(RegistryHandler.class);

    @EJB
    private ConfigBean configBean;

    @EJB
    private JobProcessor processor;

    @EJB
    private HandleQueueProcessor handleQueueProcessor;

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
    private OperationFactory operationFactory;

    public void handle(final Long registryId, final IMessenger imessenger, final FinishCallback finishLink) {
        handle(FilterWrapper.of(new RegistryRecord(registryId)), imessenger, finishLink);
    }

    private void handle(final FilterWrapper<RegistryRecord> filter, final IMessenger imessenger,
                      final FinishCallback finishHandle) {
        final AtomicBoolean finishReadRecords = new AtomicBoolean(false);
        final AtomicInteger recordHandlingCounter = new AtomicInteger(0);

        final Long registryId = filter.getObject().getRegistryId();

        imessenger.addMessageInfo("starting_handle_registries", registryId);
        finishHandle.init();

        handleQueueProcessor.execute(new AbstractJob<Void>() {
            @Override
            public Void execute() throws ExecuteException {
                try {

                    List<Registry> registries = registryBean.getRegistries(FilterWrapper.of(new Registry(registryId)));

                    // check registry exist
                    if (registries.size() == 0) {
                        imessenger.addMessageInfo("registry_not_found", registryId);
                        return null;
                    }

                    final Registry registry = registries.get(0);

                    // one process on handling
                    synchronized (this) {
                        // check registry status
                        if (!registryWorkflowManager.canProcess(registry)) {
                            imessenger.addMessageError("registry_failed_status", registryId);
                            return null;
                        }

                        // change registry status
                        if (!setHandlingStatus(registry)) {
                            imessenger.addMessageError("registry_status_inner_error", registryId);
                            return null;
                        }
                    }

                    // check registry records status
                    if (!registryRecordBean.hasRecordsToProcessing(registry)) {
                        imessenger.addMessageInfo("not_found_handle_registry_records", registryId);
                        setHandlingStatus(registry);
                        return null;
                    }

                    try {

                        final BatchProcessor<JobResult> batchProcessor = new BatchProcessor<>(10, processor);

                        int numberFlushRegistryRecords = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                        List<RegistryRecord> registryRecords;
                        FilterWrapper<RegistryRecord> innerFilter = FilterWrapper.of(filter.getObject(), 0, numberFlushRegistryRecords);
                        do {
                            final List<RegistryRecord> recordsToProcessing = registryRecordBean.getRecordsToProcessing(innerFilter);

                            if (recordsToProcessing.size() < numberFlushRegistryRecords) {
                                finishReadRecords.set(true);
                            }

                            if (recordsToProcessing.size() > 0) {

                                recordHandlingCounter.incrementAndGet();

                                batchProcessor.processJob(new AbstractJob<JobResult>() {
                                    @Override
                                    public JobResult execute() throws ExecuteException {

                                        try {
                                            List<OperationResult> results = handleRegistryRecords(registry, recordsToProcessing);

                                            return JobResult.SUCCESSFUL;
                                        } catch (Throwable th) {
                                            setErrorStatus(registry);
                                            imessenger.addMessageError("registry_failed_handle", registryId);
                                            throw new ExecuteException(th, "Failed handle registry " + registryId);
                                        } finally {
                                            if (recordHandlingCounter.decrementAndGet() == 0 && finishReadRecords.get()) {
                                                imessenger.addMessageInfo("registry_finish_handle", registryId);
                                                finishHandle.complete();
                                                setHandledStatus(registry);
                                            }
                                        }
                                    }
                                });

                                // next registry record`s id is last in this partition
                                innerFilter.setFirst(recordsToProcessing.get(recordsToProcessing.size() - 1).getId().intValue() + 1);
                            } else if (recordHandlingCounter.get() == 0) {
                                imessenger.addMessageInfo("registry_finish_handle", registryId);
                                finishHandle.complete();
                                setHandledStatus(registry);
                            }
                            registryRecords = recordsToProcessing;
                        } while (registryRecords.size() >= numberFlushRegistryRecords);

                    } catch (Throwable th) {

                        log.error("Can not handle registry " + registryId, th);

                        setErrorStatus(registry);

                    }

                } finally {
                    if (!finishReadRecords.get()) {
                        imessenger.addMessageInfo("registry_finish_handle", registryId);
                        finishHandle.complete();
                    }
                }
                return null;
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OperationResult> handleRegistryRecords(Registry registry, List<RegistryRecord> registryRecords) throws TransitionNotAllowed {
        List<OperationResult> results = Lists.newArrayListWithCapacity(registryRecords.size());
        List<OperationResult> recordResults = Lists.newArrayList();
        for (RegistryRecord registryRecord : registryRecords) {
            handelRegistryRecord(registry, results, recordResults, registryRecord);
            recordResults.clear();
        }
        return results;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void handelRegistryRecord(Registry registry,
                                     List<OperationResult> results,
                                     List<OperationResult> recordResults,
                                     RegistryRecord registryRecord) throws TransitionNotAllowed {
        try {
            for (Container container : registryRecord.getContainers()) {
                Operation operation = operationFactory.getOperation(container);
                operation.process(registry, registryRecord, container, recordResults);
            }
            results.addAll(recordResults);
        } catch (AbstractException ex) {
            log.error("Can not handleRegistryRecords", ex);
            registryRecordWorkflowManager.setNextErrorStatus(registryRecord, registry);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setHandlingStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set handling status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setHandledStatus(Registry registry) {
        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can set set handled status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setErrorStatus(Registry registry) {
        try {
            registryWorkflowManager.markProcessingHasError(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set error status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }


}
