package ru.flexpay.eirc.registry.service.link;

import ru.flexpay.eirc.registry.service.IMessenger;

/**
 * @author Pavel Sknar
 */
public class RegistryLinkerMessenger extends IMessenger {
    private static final String RESOURCE_BUNDLE = RegistryLinkerMessenger.class.getName();

    @Override
    protected String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }
}