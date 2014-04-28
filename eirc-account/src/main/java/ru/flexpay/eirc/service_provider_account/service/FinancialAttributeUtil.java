package ru.flexpay.eirc.service_provider_account.service;

import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.eirc_account.service.EircAccountUtil;
import ru.flexpay.eirc.service.service.ServiceUtil;
import ru.flexpay.eirc.service_provider_account.entity.FinancialAttribute;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;

/**
 * @author Pavel Sknar
 */
public abstract class FinancialAttributeUtil {

    public static <T extends FinancialAttribute> void addFilterMappingObject(FilterWrapper<T> filter) {

        ServiceProviderAccount serviceProviderAccount = filter == null || filter.getObject() == null? null :
                    filter.getObject().getServiceProviderAccount();

        if (serviceProviderAccount != null) {
            ServiceProviderAccountUtil.addFilterMappingObject(filter, serviceProviderAccount);
            ServiceUtil.addFilterMappingObject(filter, serviceProviderAccount.getService());
            EircAccountUtil.addFilterMappingObject(filter, serviceProviderAccount.getEircAccount());
        }

    }
}
