package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Lists;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * @author Pavel Sknar
 */
public class BatchProcessor<T> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessor.class);

    private JobProcessor processor;
    private Semaphore semaphore;

    List<CountDownLatch> waitEndWorks = Lists.newArrayList();

    public BatchProcessor(int batchSize, JobProcessor processor) {
        this.processor = processor;
        semaphore = new Semaphore(batchSize);
    }

    public Future<T> processJob(final AbstractJob<T> job) {

        AbstractJob<T> innerJob = new AbstractJob<T>() {

            @Override
            public T execute() throws ExecuteException {
                try {
                    return job.execute();
                } finally {
                    semaphore.release();
                    jobFinalize();
                }
            }
        };

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            //
        }

        return processor.processJob(innerJob);

    }

    public void waitEndWorks() {
        while (semaphore.getQueueLength() > 0) {
            CountDownLatch waitEndWork;
            waitEndWork  = new CountDownLatch(semaphore.getQueueLength());
            log.debug("create latch: {}", waitEndWork);
            waitEndWorks.add(waitEndWork);
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