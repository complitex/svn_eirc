package ru.flexpay.eirc.registry.service;

import javax.ejb.Singleton;

/**
 * @author Pavel Sknar
 */
@Singleton
public class RegistryMessenger extends IMessenger {
    private static final String RESOURCE_BUNDLE = RegistryMessenger.class.getName();

    @Override
    protected String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }
}
