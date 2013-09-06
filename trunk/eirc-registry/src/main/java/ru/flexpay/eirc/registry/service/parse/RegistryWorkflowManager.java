package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryStatus;
import ru.flexpay.eirc.registry.service.RegistryBean;
import ru.flexpay.eirc.registry.service.RegistryRecordBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.flexpay.eirc.registry.entity.RegistryStatus.*;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryWorkflowManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @EJB
    private RegistryBean registryBean;

    @EJB
    private RegistryRecordBean registryRecordBean;

    // allowed transitions from source status code to target codes
    // first status in lists is the successfull one, the second is transition with some processing error
    private static final Map<RegistryStatus, List<RegistryStatus>> transitions = Maps.newHashMap();

    private static final Set<RegistryStatus> transitionsToProcessing =
            Sets.newHashSet(LOADED, PROCESSED_WITH_ERROR, PROCESSING_CANCELED, ROLLBACKED);

    static {
        transitions.put(LOADING, ImmutableList.of(LOADED, LOADED_WITH_ERROR));

        transitions.put(LOADED_WITH_ERROR, Collections.<RegistryStatus>emptyList());

        transitions.put(LOADED, ImmutableList.of(PROCESSING));

        transitions.put(PROCESSING, ImmutableList.of(PROCESSED, PROCESSING_WITH_ERROR, PROCESSING_CANCELED));

        // allow set processed with errors if there are any not processed records
        transitions.put(PROCESSED, ImmutableList.of(ROLLBACKING, PROCESSED_WITH_ERROR));

        transitions.put(PROCESSING_WITH_ERROR, ImmutableList.of(PROCESSED_WITH_ERROR, PROCESSING_CANCELED));

        transitions.put(PROCESSED_WITH_ERROR, ImmutableList.of(PROCESSING, ROLLBACKING));

        transitions.put(PROCESSING_CANCELED, ImmutableList.of(PROCESSING, ROLLBACKING));

        transitions.put(ROLLBACKING, ImmutableList.of(ROLLBACKED));

        transitions.put(ROLLBACKED, ImmutableList.of(PROCESSING));
    }

    /**
     * Check if registry status transition allowed
     *
     * @param registry   Registry to check
     * @param nextStatus Registry status to set up
     * @return <code>true</code> if registry processing allowed, or <code>false</code> otherwise
     */
    public boolean canTransit(Registry registry, RegistryStatus nextStatus) {
        StringBuilder stack = new StringBuilder();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            stack.append("\t").append(element.toString()).append("\n");
        }
        if (log.isDebugEnabled()) {
            log.debug("Stack thread: {}", stack.toString());
            log.debug("Current status is {}. Next status is {}. Allowed transition is {}",
                    new Object[] {registry.getStatus(), nextStatus, transitions.get(registry.getStatus())});
        }
        return transitions.get(registry.getStatus()).contains(nextStatus);
    }

    /**
     * Check if registry can be processed, i.e. has one of the following statuses: {@link
     * ru.flexpay.eirc.registry.entity.RegistryStatus#LOADED}, {@link
     * ru.flexpay.eirc.registry.entity.RegistryStatus#ROLLBACKED},
     * {@link ru.flexpay.eirc.registry.entity.RegistryStatus#PROCESSING_CANCELED} or {@link
     * ru.flexpay.eirc.registry.entity.RegistryStatus#PROCESSED_WITH_ERROR}
     *
     * @param registry Registry to check
     * @return <code>true</code> if registry is allowed to be processed
     */
    public boolean canProcess(Registry registry) {
        return transitionsToProcessing.contains(registry.getStatus());
    }

    public void startProcessing(Registry registry) throws TransitionNotAllowed {
        if (!canProcess(registry)) {
            throw new TransitionNotAllowed("Cannot start registry processing",
                    registry.getStatus());
        }

        setNextSuccessStatus(registry);
    }

    public void endProcessing(Registry registry) throws TransitionNotAllowed {
        // all records processed
        if (registry.getStatus() == PROCESSED) {
            try {
                if (registryRecordBean.hasRecordsToProcessing(registry)) {
                    setNextStatus(registry, PROCESSED_WITH_ERROR);
                }
            } catch (Throwable t) {
                setNextStatus(registry, PROCESSED_WITH_ERROR);
                log.error("Unexpected error", t);
                throw new RuntimeException("Unexpected error when ending processing", t);
            }
        }
    }

    /**
     * Set next error registry status
     *
     * @param registry Registry to update
     * @throws TransitionNotAllowed if error transition is not allowed
     */
    public void setNextErrorStatus(Registry registry) throws TransitionNotAllowed {
        List<RegistryStatus> allowedCodes = transitions.get(registry.getStatus());
        if (allowedCodes.size() < 2) {
            throw new TransitionNotAllowed("No error transition", registry.getStatus());
        }

        setNextStatus(registry, allowedCodes.get(1));
    }


    /**
     * Set next success registry status
     *
     * @param registry Registry to update
     * @throws TransitionNotAllowed if success transition is not allowed
     */
    public void setNextSuccessStatus(Registry registry) throws TransitionNotAllowed {
        List<RegistryStatus> allowedCodes = transitions.get(registry.getStatus());
        if (allowedCodes.size() < 1) {
            throw new TransitionNotAllowed("No success transition", registry.getStatus());
        }

        setNextStatus(registry, allowedCodes.get(0));
    }

    /**
     * Set initial registry status
     *
     * @param registry Registry to update
     * @return SpRegistry back
     * @throws TransitionNotAllowed if registry already has a status
     */
    public Registry setInitialStatus(Registry registry) throws TransitionNotAllowed {
        if (registry.getStatus() != null) {
            if (registry.getStatus() != LOADING) {
                throw new TransitionNotAllowed("Registry was already processed, cannot set initial status");
            }

            return registry;
        }

        registry.setStatus(LOADING);
        return registry;
    }

    /**
     * Set next registry status
     *
     * @param registry Registry to update
     * @param status   Next status to set
     * @throws TransitionNotAllowed if transition from old to a new status is not allowed
     */
    public void setNextStatus(Registry registry, RegistryStatus status) throws TransitionNotAllowed {

        registry.setStatus(status);
        try {
            registryBean.update(registry);
        } catch (Throwable e) {
            log.debug("Transition not allowed because {}", e);
            throw new TransitionNotAllowed("Invalid update registry: " + registry.toString());
        }
    }

    /**
     * Set registry processing status to {@link ru.flexpay.eirc.registry.entity.RegistryStatus#PROCESSING_WITH_ERROR}
     *
     * @param registry Registry to update
     * @throws TransitionNotAllowed if registry status is not {@link ru.flexpay.eirc.registry.entity.RegistryStatus#PROCESSING}
     *                              or {@link ru.flexpay.eirc.registry.entity.RegistryStatus#PROCESSING_WITH_ERROR}
     */
    public void markProcessingHasError(Registry registry) throws TransitionNotAllowed {
        if (!transitions.get(PROCESSING).contains(registry.getStatus())) {
            throw new TransitionNotAllowed("Cannot mark not processing registry as having errors. Current registry code", registry.getStatus());
        }

        log.debug("Setting registry errors: {}", registry);

        if (registry.getStatus() == PROCESSING) {
            log.debug("Updating registry status");
            setNextErrorStatus(registry);
        } else {
            log.debug("Not updating registry status, current is {}", registry.getStatus());
        }
    }
}
