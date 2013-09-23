package ru.flexpay.eirc.registry.service.parse;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryParserMessenger extends IMessenger {
    private static final String RESOURCE_BUNDLE = RegistryParserMessenger.class.getName();

    @Override
    protected String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }
}
