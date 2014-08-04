package ru.flexpay.eirc.registry.service;

import ch.qos.cal10n.IMessageConveyor;
import org.slf4j.LoggerFactory;
import org.slf4j.cal10n.LocLogger;

/**
 * @author Pavel Sknar
 */
public class RegistryLocLoggerFactory extends org.slf4j.cal10n.LocLoggerFactory {
    private IMessageConveyor imc;

    private AbstractMessenger messenger;

    private Long registryId;

    public RegistryLocLoggerFactory(IMessageConveyor imc, AbstractMessenger messenger, Long registryId) {
        super(imc);
        this.imc = imc;
        this.messenger = messenger;
        this.registryId = registryId;
    }

    /**
     * Get an LocLogger instance by name.
     *
     * @param name
     * @return LocLogger instance by name.
     */
    @Override
    public LocLogger getLocLogger(String name) {
        return new RegistryLogger(LoggerFactory.getLogger(name), imc, registryId, messenger);
    }

    /**
     * Get a new LocLogger instance by class. The returned LocLogger will be named
     * after the class.
     *
     * @param clazz
     * @return LocLogger instance by class
     */
    @Override
    public LocLogger getLocLogger(Class clazz) {
        return getLocLogger(clazz.getName());
    }
}
