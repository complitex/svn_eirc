package ru.flexpay.eirc.registry.service.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.service.JobProcessor;
import ru.flexpay.eirc.registry.service.QueueProcessor;

import javax.annotation.PostConstruct;
import javax.ejb.*;

/**
 * @author Pavel Sknar
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MbConverterQueueProcessor extends QueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(MbConverterQueueProcessor.class);

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
