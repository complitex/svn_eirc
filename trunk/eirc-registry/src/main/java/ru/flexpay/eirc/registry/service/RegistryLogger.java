package ru.flexpay.eirc.registry.service;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.cal10n.LocLogger;

import java.util.Locale;

/**
* @author Pavel Sknar
*/
public class RegistryLogger extends LocLogger {

    // create a message conveyor for a given locale
    private static final IMessageConveyor messageConveyor = new MessageConveyor(new Locale("ru"));

    private AbstractMessenger messenger;

    private Long registryId;

    RegistryLogger(Logger logger, IMessageConveyor imc, Long registryId, AbstractMessenger messenger) {
        super(logger, imc);
        this.registryId = registryId;
        this.messenger = messenger;
    }

    @Override
    public void trace(Enum<?> key, Object... args) {
        super.trace(key, addAdditionalArguments(args));
    }

    @Override
    public void debug(Enum<?> key, Object... args) {
        super.debug(key, addAdditionalArguments(args));
    }

    @Override
    public void info(Enum<?> key, Object... args) {
        args = addAdditionalArguments(args);
        if (messenger != null) {
            messenger.addMessageInfo(key, args);
        }
        super.info(key, args);
    }

    @Override
    public void warn(Enum<?> key, Object... args) {
        super.warn(key, addAdditionalArguments(args));
    }

    @Override
    public void error(Enum<?> key, Object... args) {
        args = addAdditionalArguments(args);
        if (messenger != null) {
            messenger.addMessageError(key, args);
        }
        super.error(key, args);
    }
    
    private Object[] addAdditionalArguments(Object... args) {
        return registryId != null && registryId > 0 ? ArrayUtils.add(args, registryId) : args;
    }

    public static LocLogger getInstance(Long registryId, AbstractMessenger messenger, Class<?> clazz) {
        return new RegistryLocLoggerFactory(messageConveyor, messenger, registryId).getLocLogger(clazz);
    }
}
