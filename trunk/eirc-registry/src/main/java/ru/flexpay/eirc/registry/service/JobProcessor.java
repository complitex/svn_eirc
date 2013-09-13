package ru.flexpay.eirc.registry.service;

import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.concurrent.Future;

/**
 * @author Pavel Sknar
 */
@Stateless
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    @Asynchronous
    public <T> Future<T> processJob(AbstractJob<T> job) {

        T result;
        try {
            result = job.execute();
        } catch (ExecuteException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        }

        return new AsyncResult<>(result);

    }
}

