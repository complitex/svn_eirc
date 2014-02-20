package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class BaseAccountOperation extends Operation {

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    public ServiceProviderAccount getServiceProviderAccount(Registry registry, RegistryRecordData registryRecord) throws DataNotFoundException {
        Address address = registryRecord.getAddress();
        String serviceProviderAccountNumber = registryRecord.getPersonalAccountExt();
        Long organizationId = registry.getType().isPayments()? registry.getRecipientOrganizationId() : registry.getSenderOrganizationId();
        String serviceCode = registryRecord.getServiceCode();

        if (address == null) {
            throw new DataNotFoundException("Address empty in registry record: {0}", registryRecord);
        }

        EircAccount eircAccount = new EircAccount();
        eircAccount.setAddress(address);
        ServiceProviderAccount serviceProviderAccount = new ServiceProviderAccount(eircAccount);
        serviceProviderAccount.setService(new Service(serviceCode));
        serviceProviderAccount.setOrganizationId(organizationId);
        serviceProviderAccount.setAccountNumber(serviceProviderAccountNumber);

        FilterWrapper<ServiceProviderAccount> filter = FilterWrapper.of(serviceProviderAccount);
        filter.setSortProperty(null);

        List<ServiceProviderAccount> serviceProviderAccounts =
                serviceProviderAccountBean.getServiceProviderAccounts(filter);
        if (serviceProviderAccounts.size() == 0) {
            throw new DataNotFoundException("Not found service provider account by filter: {0}", filter);
        }
        return serviceProviderAccounts.get(0);
    }

}
