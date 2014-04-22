package ru.flexpay.eirc.service.service;

import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.service.entity.Service;

/**
 * @author Pavel Sknar
 */
public abstract class ServiceUtil {
    public static void addFilterMappingObject(FilterWrapper<Service> filter) {
        if (filter != null) {
            addFilterMappingObject(filter, filter.getObject());
        }
    }

    public static void addFilterMappingObject(FilterWrapper<?> filter, Service service) {
        if (filter != null) {
            filter.getMap().put(ServiceBean.FILTER_MAPPING_ATTRIBUTE_NAME, service);
        }
    }
}
