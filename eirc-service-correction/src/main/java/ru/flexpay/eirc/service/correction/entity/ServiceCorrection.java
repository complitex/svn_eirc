package ru.flexpay.eirc.service.correction.entity;

import org.complitex.dictionary.entity.Correction;

/**
 * @author Pavel Sknar
 */
public class ServiceCorrection extends Correction {
    public ServiceCorrection() {
    }

    public ServiceCorrection(String externalId, Long objectId, String correction, Long organizationId,
                                  Long userOrganizationId, Long moduleId) {
        super(externalId, objectId, correction, organizationId, userOrganizationId, moduleId);
    }

    @Override
    public String getEntity() {
        return "service";
    }
}
