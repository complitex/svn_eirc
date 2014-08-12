package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import org.complitex.dictionary.service.SessionBean;

import javax.ejb.EJB;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
public abstract class FinishCallback extends AbstractFinishCallback {

    private Map<Long, AtomicInteger> counters = Maps.newConcurrentMap();
    private Map<Long, Set<Long>> processIds = Maps.newConcurrentMap();

    @EJB
    private SessionBean sessionBean;

    public AbstractFinishCallback getInstance() {
        AtomicInteger counter = getUserCounter();
        Set<Long> processIds = getUserProcessIds();

        return new SimpleFinishCallback(counter, processIds);
    }

    @Override
    public boolean isCompleted() {
        for (Set<Long> userProcessIds : processIds.values()) {
            if (!userProcessIds.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isCompleted(long processId) {
        for (Set<Long> userProcessIds : processIds.values()) {
            if (userProcessIds.contains(processId)) {
                return false;
            }
        }
        return true;
    }

    public boolean canCanceled(long processId) {
        return sessionBean.isAdmin() && !isCompleted(processId) || getUserProcessIds().contains(processId);
    }

    @Override
    public void setProcessId(Long processId) {
        throw new UnsupportedOperationException("use getInstance()");
    }

    @Override
    protected AtomicInteger getCounter() {
        return getUserCounter();
    }

    private Set<Long> getUserProcessIds() {
        Long userId = sessionBean.getCurrentUserId();
        Set<Long> userProcessIds = processIds.get(userId);
        if (userProcessIds == null) {
            userProcessIds = Collections.newSetFromMap(Maps.<Long, Boolean>newConcurrentMap());
            processIds.put(userId, userProcessIds);
        }
        return userProcessIds;
    }

    private AtomicInteger getUserCounter() {
        Long userId = sessionBean.getCurrentUserId();
        AtomicInteger userCounter = counters.get(userId);
        if (userCounter == null) {
            userCounter = new AtomicInteger(0);
            counters.put(userId, userCounter);
        }
        return userCounter;
    }

    private class SimpleFinishCallback extends AbstractFinishCallback {
        private AtomicInteger counter;
        private Set<Long> userProcessIds;
        private Long processId;

        private SimpleFinishCallback(AtomicInteger counter, Set<Long> userProcessIds) {
            this.counter = counter;
            this.userProcessIds = userProcessIds;
        }

        @Override
        public void complete() {
            if (processId != null) {
                userProcessIds.remove(processId);
                processId = null;
            }
            super.complete();
        }

        @Override
        protected AtomicInteger getCounter() {
            return counter;
        }

        @Override
        public void setProcessId(Long processId) {
            if (this.processId != null) {
                throw new IllegalStateException("processId was set");
            }
            this.processId = processId;
            userProcessIds.add(processId);
        }
    }

}
