package ru.flexpay.eirc.registry.service.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.service.JobProcessor;
import ru.flexpay.eirc.registry.service.QueueProcessor;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * @author Pavel Sknar
 */
@Singleton
public class ParserQueueProcessor extends QueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(ParserQueueProcessor.class);

    @EJB
    private JobProcessor processor;

    @PostConstruct
    @Override
    public void run() throws InterruptedException {
        setProcessor(processor);
        log.info("run");
        super.run();
        log.info("runned");
    }
}
