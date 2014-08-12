package ru.flexpay.eirc.registry.service;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
public abstract class AbstractFinishCallback implements Serializable {

    public void init() {
        getCounter().incrementAndGet();
    }

    public void init(Long processId) {
        setProcessId(processId);
        init();
    }

    public void complete() {
        getCounter().decrementAndGet();
    }

    public void cancel() {

    }

    public boolean isCompleted() {
        return getCounter().get() <= 0;
    }

    public abstract void setProcessId(Long processId);

    protected abstract AtomicInteger getCounter();
}
