package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.util.CloneUtil;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class ServiceProviderAccountAttrOperation extends BaseAccountOperation {

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private ServiceProviderAccountStrategy serviceProviderAccountStrategy;

    @EJB
    private LocaleBean localeBean;

    @Override
    public void process(Registry registry, RegistryRecordData registryRecord, Container container,
                        List<OperationResult> results) throws AbstractException {

        Address address = registryRecord.getAddress();
        String serviceProviderAccountNumber = registryRecord.getPersonalAccountExt();
        Long organizationId = registry.getSenderOrganizationId();
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

        DomainObject newObject = serviceProviderAccountStrategy.findById(serviceProviderAccounts.get(0).getId(), true);
        DomainObject oldObject = CloneUtil.cloneObject(newObject);

        Attribute numberOfHabitants = newObject.getAttribute(getAttributeId());

        BaseAccountOperationData data = getContainerData(container);

        AttributeUtil.setStringValue(numberOfHabitants, data.getNewValue(), localeBean.getSystemLocaleObject().getId());

        serviceProviderAccountStrategy.update(oldObject, newObject, data.getChangeApplyingDate());

        results.add(new OperationResult<>(oldObject, newObject, getCode()));
    }

    protected abstract Long getAttributeId();
}
