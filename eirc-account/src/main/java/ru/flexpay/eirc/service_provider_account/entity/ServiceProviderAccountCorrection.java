package ru.flexpay.eirc.service_provider_account.entity;

import org.complitex.dictionary.entity.Correction;

/**
 * @author Pavel Sknar
 */
public class ServiceProviderAccountCorrection extends Correction {
    public ServiceProviderAccountCorrection() {
    }

    public ServiceProviderAccountCorrection(String externalId, Long objectId, String correction, Long organizationId,
                                            Long userOrganizationId, Long moduleId) {
        super(externalId, objectId, correction, organizationId, userOrganizationId, moduleId);
    }

    @Override
    public String getEntity() {
        return "service_provider_account";
    }
}
