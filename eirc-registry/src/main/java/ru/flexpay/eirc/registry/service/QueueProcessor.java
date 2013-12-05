package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Queues;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * @author Pavel Sknar
 */
public abstract class QueueProcessor {

    private final Logger log = LoggerFactory.getLogger(QueueProcessor.class);

    private Queue<AbstractJob<Void>> jobs = Queues.newConcurrentLinkedQueue();

    private int MAX_AVAILABLE = 10;
    private final Semaphore available = new Semaphore(0, false);
    private final Semaphore fullQueue = new Semaphore(MAX_AVAILABLE, false);

    private JobProcessor processor;

    public QueueProcessor() {
    }

    protected void setProcessor(JobProcessor processor) {
        this.processor = processor;
    }

    private AbstractJob<Void> getItem() throws InterruptedException {
        available.acquire();
        fullQueue.acquire();
        return jobs.poll();
    }

    public void execute(AbstractJob<Void> job) {
        jobs.add(job);
        available.release();
    }

    public void run() throws InterruptedException {
        processor.processJob(new AbstractJob<Object>() {
            public Object execute() throws ExecuteException {
                while (true) {

                    final AbstractJob<Void> job;
                    try {
                        job = getItem();
                    } catch (InterruptedException e) {
                        throw new ExecuteException(e, "Interrupted");
                    }
                    processor.processJob(new AbstractJob<Object>() {
                        @Override
                        public Object execute() throws ExecuteException {
                            try {
                                return job.execute();
                            } finally {
                                fullQueue.release();
                            }
                        }
                    });

                }
            }
        });
    }

}
