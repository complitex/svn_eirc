package ru.flexpay.eirc.eirc_account.service;

import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;

/**
 * @author Pavel Sknar
 */
public abstract class EircAccountUtil {
    public static void addFilterMappingObject(FilterWrapper<EircAccount> filter) {
        if (filter != null) {
            addFilterMappingObject(filter, filter.getObject());
        }
    }

    public static void addFilterMappingObject(FilterWrapper<?> filter, EircAccount eircAccount) {
        if (filter != null) {
            filter.getMap().put(EircAccountBean.FILTER_MAPPING_ATTRIBUTE_NAME, eircAccount);
        }
    }
}
