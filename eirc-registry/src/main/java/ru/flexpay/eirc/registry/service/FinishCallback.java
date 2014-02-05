package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import org.complitex.dictionary.service.SessionBean;

import javax.ejb.EJB;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
public abstract class FinishCallback extends AbstractFinishCallback {

    private Map<Long, AtomicInteger> counters = Maps.newConcurrentMap();

    @EJB
    private SessionBean sessionBean;

    public AbstractFinishCallback getInstance() {
        AtomicInteger counter = getUserCounter();

        return new SimpleFinishCallback(counter);
    }

    @Override
    protected AtomicInteger getCounter() {
        return getUserCounter();
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

    private static class SimpleFinishCallback extends AbstractFinishCallback {
        private AtomicInteger counter;

        private SimpleFinishCallback(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        protected AtomicInteger getCounter() {
            return counter;
        }
    }

}
