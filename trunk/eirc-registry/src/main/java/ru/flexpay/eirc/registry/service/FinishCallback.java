package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import org.complitex.dictionary.service.SessionBean;

import javax.ejb.EJB;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
public class FinishCallback {

    private Map<Long, AtomicInteger> counters = Maps.newConcurrentMap();

    @EJB
    private SessionBean sessionBean;

    public void init() {
        getUserCounter().incrementAndGet();
    }

    public void complete() {
        getUserCounter().decrementAndGet();
    }

    public boolean isCompleted() {
        return getUserCounter().get() <= 0;
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

}
