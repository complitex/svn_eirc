package ru.flexpay.eirc.service_provider_account.service;

import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.eirc_account.service.EircAccountUtil;
import ru.flexpay.eirc.service.service.ServiceUtil;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;

/**
 * @author Pavel Sknar
 */
public abstract class ServiceProviderAccountUtil {

    public static void addFilterMappingObject(FilterWrapper<ServiceProviderAccount> filter) {
        if (filter != null) {
            if (filter.getObject() != null) {
                ServiceUtil.addFilterMappingObject(filter, filter.getObject().getService());
                EircAccountUtil.addFilterMappingObject(filter, filter.getObject().getEircAccount());
            }
            addFilterMappingObject(filter, filter.getObject());
        }
    }

    public static void addFilterMappingObject(FilterWrapper<?> filter,
                                              ServiceProviderAccount serviceProviderAccount) {
        if (filter != null) {
            filter.getMap().put(ServiceProviderAccountBean.FILTER_MAPPING_ATTRIBUTE_NAME, serviceProviderAccount);
        }
    }
}
