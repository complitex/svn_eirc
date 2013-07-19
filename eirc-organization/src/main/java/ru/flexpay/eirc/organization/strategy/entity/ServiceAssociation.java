/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.organization.strategy.entity;

import java.io.Serializable;

/**
 *
 * @author Artem
 */
public class ServiceAssociation implements Serializable {

    private Long id;
    private Long serviceProviderTypeId;
    private Long calculationCenterId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCalculationCenterId() {
        return calculationCenterId;
    }

    public void setCalculationCenterId(Long calculationCenterId) {
        this.calculationCenterId = calculationCenterId;
    }

    public Long getServiceProviderTypeId() {
        return serviceProviderTypeId;
    }

    public void setServiceProviderTypeId(Long serviceProviderTypeId) {
        this.serviceProviderTypeId = serviceProviderTypeId;
    }

    @Override
    public int hashCode() {
        long h = 7;
        h = 31 * h + (serviceProviderTypeId != null ? serviceProviderTypeId : 0);
        h = 31 * h + (calculationCenterId != null ? calculationCenterId : 0);
        return (int) h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        final ServiceAssociation that = (ServiceAssociation) o;
        if (serviceProviderTypeId != that.serviceProviderTypeId && (serviceProviderTypeId == null
                || !serviceProviderTypeId.equals(that.serviceProviderTypeId))) {
            return false;
        }
        if (calculationCenterId != that.calculationCenterId && (calculationCenterId == null
                || !calculationCenterId.equals(that.calculationCenterId))) {
            return false;
        }
        return true;
    }
}
