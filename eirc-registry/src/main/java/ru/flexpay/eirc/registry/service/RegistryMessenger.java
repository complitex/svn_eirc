package ru.flexpay.eirc.registry.service;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Pavel Sknar
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RegistryMessenger extends IMessenger {
    private static final String RESOURCE_BUNDLE = RegistryMessenger.class.getName();

    @Override
    public String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }
}
