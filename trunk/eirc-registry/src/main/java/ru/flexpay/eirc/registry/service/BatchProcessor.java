package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Lists;
import org.complitex.dictionary.service.executor.ExecuteException;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author Pavel Sknar
 */
public class BatchProcessor<T> implements Serializable {

    private JobProcessor processor;
    private int batchSize = 10;
    private volatile int countWorker = 0;

    List<CountDownLatch> waitEndWorks = Lists.newArrayList();

    public BatchProcessor(int batchSize, JobProcessor processor) {
        this.batchSize = batchSize;
        this.processor = processor;
    }

    public Future<T> processJob(final AbstractJob<T> job) {

        final CountDownLatch latch = new CountDownLatch(1);

        AbstractJob<T> innerJob = new AbstractJob<T>() {

            @Override
            public T execute() throws ExecuteException {
                try {
                    return job.execute();
                } finally {
                    countWorker--;
                    latch.countDown();
                    jobFinalize();
                }
            }
        };

        if (countWorker >= batchSize) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                //
            }
        }
        countWorker++;

        return processor.processJob(innerJob);

    }

    public void waitEndWorks() {
        while (countWorker > 0) {
            CountDownLatch waitEndWork;
            synchronized (this) {
                waitEndWork  = new CountDownLatch(countWorker);
                waitEndWorks.add(waitEndWork);
            }
            try {
                waitEndWork.await();
            } catch (InterruptedException e) {
                //
            }
        }
    }

    protected void jobFinalize() {
        synchronized (this) {
            for (CountDownLatch waitEndWork : waitEndWorks) {
                waitEndWork.countDown();
            }
        }
    }
}