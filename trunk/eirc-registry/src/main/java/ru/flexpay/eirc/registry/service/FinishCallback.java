package ru.flexpay.eirc.registry.service;

/**
 * @author Pavel Sknar
 */
public class FinishCallback {

    private volatile int count = 0;
    private volatile boolean changed = false;

    public void init() {
        count++;
        changed = true;
    }

    public void complete() {
        count--;
    }

    public boolean isCompleted() {
        try {
            return count <= 0 && changed;
        } finally {
            changed = false;
        }
    }

}
