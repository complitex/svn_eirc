package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Sets;
import org.complitex.dictionary.service.executor.ExecuteException;

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
    public boolean isCanceling(long processId) {
        return processIds.contains(processId);
    }

    /**
     * Cancel processId. If need canceled execute <code>runnable</code> task
     *
     * @param processId process id
     * @param runnable Execute code
     * @return <code>True</code> if process canceled otherwise <code>False</code>
     * @throws ExecuteException if <code>runnable</code> was executed with exception
     */
    @Lock
    public boolean isCancel(long processId, Runnable runnable) throws ExecuteException {
        if (!processIds.contains(processId)) {
            return false;
        }
        try {
            runnable.run();
        } catch (Throwable th) {
            throw new ExecuteException(th, "Can not canceled process {0}", processId);
        }
        processIds.remove(processId);
        return true;
    }
}
