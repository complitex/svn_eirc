package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Sets;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import java.util.Set;

/**
 * @author Pavel Sknar
 */
@ConcurrencyManagement
@Singleton
public class CanceledProcessing {
    private final Set<Long> processIds = Sets.newHashSet();

    @Lock
    public void cancel(long processId) {
        processIds.add(processId);
    }

    @Lock
    public boolean isCancel(long processId) {
        boolean result = processIds.contains(processId);
        if (result) {
            processIds.remove(processId);
        }
        return  result;
    }
}
