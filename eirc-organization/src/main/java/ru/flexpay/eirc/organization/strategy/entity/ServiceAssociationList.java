/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.organization.strategy.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Artem
 */
public final class ServiceAssociationList extends ArrayList<ServiceAssociation> {

    public ServiceAssociationList(List<ServiceAssociation> serviceAssociations) {
        super(serviceAssociations);
    }

    public ServiceAssociationList() {
        super(new ArrayList<ServiceAssociation>());
    }

    public void addNew() {
        add(new ServiceAssociation());
    }

    public boolean containsServiceProviderType(long serviceProviderTypeId) {
        for (ServiceAssociation serviceAssociation : this) {
            if (new Long(serviceProviderTypeId).equals(serviceAssociation.getServiceProviderTypeId())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNulls() {
        for (ServiceAssociation serviceAssociation : this) {
            if (serviceAssociation == null || serviceAssociation.getServiceProviderTypeId() == null
                    || serviceAssociation.getCalculationCenterId() == null) {
                return true;
            }
        }
        return false;
    }

    public Set<Long> getDuplicateServiceProviderTypeIds() {
        final Multimap<Long, Long> serviceProviderTypeToCalculationCentres = ArrayListMultimap.create();

        for (ServiceAssociation serviceAssociation : this) {
            if (serviceAssociation != null && serviceAssociation.getServiceProviderTypeId() != null) {
                serviceProviderTypeToCalculationCentres.put(serviceAssociation.getServiceProviderTypeId(),
                        serviceAssociation.getCalculationCenterId());
            }
        }

        Set<Long> result = Sets.newHashSet();

        for (Entry<Long> e : serviceProviderTypeToCalculationCentres.keys().entrySet()) {
            if (e.getCount() > 1) {
                result.add(e.getElement());
            }
        }

        return result.isEmpty() ? null : result;
    }

    public SetMultimap<Long, Long> groupByCalculationCenter() {
        final SetMultimap<Long, Long> groups = HashMultimap.create();

        for (ServiceAssociation serviceAssociation : this) {
            if (serviceAssociation != null && serviceAssociation.getCalculationCenterId() != null
                    && serviceAssociation.getServiceProviderTypeId() != null) {
                groups.put(serviceAssociation.getCalculationCenterId(), serviceAssociation.getServiceProviderTypeId());
            }
        }

        return ImmutableSetMultimap.copyOf(groups);
    }

    @Override
    public int hashCode() {
        int h = 0;
        Iterator<ServiceAssociation> i = iterator();
        while (i.hasNext()) {
            ServiceAssociation obj = i.next();
            if (obj != null) {
                h += obj.hashCode();
            }
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        ServiceAssociationList that = (ServiceAssociationList) o;
        if (that.size() != size()) {
            return false;
        }
        return containsAll(that);
    }
}
