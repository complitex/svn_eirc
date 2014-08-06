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

    public void complete() {
        getCounter().decrementAndGet();
    }

    public void cancel() {

    }

    public boolean isCompleted() {
        return getCounter().get() <= 0;
    }

    protected abstract AtomicInteger getCounter();
}
