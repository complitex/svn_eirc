package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Lists;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author Pavel Sknar
 */
public class BatchProcessor<T> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessor.class);

    private JobProcessor processor;
    private int batchSize;
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
                    latch.countDown();
                    synchronized (this) {
                        countWorker--;
                        jobFinalize();
                    }
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
                log.debug("create latch: {}", waitEndWork);
                waitEndWorks.add(waitEndWork);
            }
            try {
                waitEndWork.await();
                log.debug("finish latch: {}", waitEndWork);
            } catch (InterruptedException e) {
                //
            }
        }
    }

    protected void jobFinalize() {
        log.debug("finalize job");
        for (CountDownLatch waitEndWork : waitEndWorks) {
            waitEndWork.countDown();
            log.debug("finalize latch: {}", waitEndWork);
        }
    }
}