package ru.flexpay.eirc.registry.service.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.service.JobProcessor;
import ru.flexpay.eirc.registry.service.QueueProcessor;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * @author Pavel Sknar
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LinkQueueProcessor extends QueueProcessor {

    private final Logger log = LoggerFactory.getLogger(LinkQueueProcessor.class);

    @EJB
    private JobProcessor processor;

    @PostConstruct
    @Override
    public void run() {
        setProcessor(processor);
        log.info("run");
        try {
            super.run();
        } catch (InterruptedException e) {
            //
        }
        log.info("runned");
    }
}