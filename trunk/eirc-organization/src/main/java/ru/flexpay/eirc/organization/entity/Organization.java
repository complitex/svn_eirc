package ru.flexpay.eirc.organization.entity;

import com.google.common.collect.Lists;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class Organization extends DomainObject {

    public Organization(DomainObject copy) {
        super(copy);
    }

    public List<OrganizationType> getOrganizationTypes() {
        List<Attribute> types = getAttributes(IOrganizationStrategy.ORGANIZATION_TYPE);
        List<OrganizationType> result = Lists.newArrayListWithExpectedSize(types.size());
        for (Attribute type : types) {
            if (type.getValueId().equals(OrganizationType.SERVICE_PROVIDER.getId())) {
                result.add(OrganizationType.SERVICE_PROVIDER);
            } else if (type.getValueId().equals(OrganizationType.USER_ORGANIZATION.getId())) {
                result.add(OrganizationType.USER_ORGANIZATION);
            } else if (type.getValueId().equals(OrganizationType.PAYMENT_COLLECTOR.getId())) {
                result.add(OrganizationType.PAYMENT_COLLECTOR);
            }
        }
        return result;
    }

    public boolean isServiceProvider() {
        List<Attribute> types = getAttributes(IOrganizationStrategy.ORGANIZATION_TYPE);
        for (Attribute type : types) {
            if (type.getValueId() != null &&
                    type.getValueId().equals(OrganizationType.SERVICE_PROVIDER.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPaymentCollector() {
        List<Attribute> types = getAttributes(IOrganizationStrategy.ORGANIZATION_TYPE);
        for (Attribute type : types) {
            if (type.getValueId() != null &&
                    type.getValueId().equals(OrganizationType.PAYMENT_COLLECTOR.getId())) {
                return true;
            }
        }
        return false;
    }
}
