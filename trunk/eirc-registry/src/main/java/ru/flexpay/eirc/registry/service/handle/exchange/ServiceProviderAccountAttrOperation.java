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
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class ServiceProviderAccountAttrOperation extends Operation<DomainObject> {

    private ServiceProviderAccountBean serviceProviderAccountBean;

    private ServiceProviderAccountStrategy serviceProviderAccountStrategy;

    private LocaleBean localeBean;

    private Address address;

    private String serviceProviderAccountNumber;
    private Long organizationId;
    private String serviceCode;

    private DomainObject newObject;
    private DomainObject oldObject;

    /**
     * Parse data and set operation id. Executing {@link ru.flexpay.eirc.registry.service.handle.exchange.Operation#prepareData}
     *
     * @param container Container
     * @throws org.complitex.dictionary.service.exception.AbstractException
     *
     */
    public ServiceProviderAccountAttrOperation(Registry registry, RegistryRecord registryRecord, Container container) throws AbstractException {
        super(container);
        address = registryRecord.getAddress();
        serviceProviderAccountNumber = registryRecord.getPersonalAccountExt();
        organizationId = registry.getSenderOrganizationId();
        serviceCode = registryRecord.getServiceCode();

        if (address == null) {
            throw new DataNotFoundException("Address empty in registry record: {0}", registryRecord);
        }
    }

    @Override
    public DomainObject getOldObject() {
        return oldObject;
    }

    @Override
    public DomainObject getNewObject() {
        return newObject;
    }

    @Override
    public void process() throws AbstractException {
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

        newObject = serviceProviderAccountStrategy.findById(serviceProviderAccounts.get(0).getId(), true);
        oldObject = CloneUtil.cloneObject(newObject);

        Attribute numberOfHabitants = newObject.getAttribute(getAttributeId());

        AttributeUtil.setStringValue(numberOfHabitants, getNewValue(), localeBean.getSystemLocaleObject().getId());

        serviceProviderAccountStrategy.update(oldObject, newObject, getChangeApplyingDate());
    }

    @Override
    protected void prepareData(List<String> containerData) throws AbstractException {

    }

    public void setServiceProviderAccountBean(ServiceProviderAccountBean serviceProviderAccountBean) {
        this.serviceProviderAccountBean = serviceProviderAccountBean;
    }

    public void setServiceProviderAccountStrategy(ServiceProviderAccountStrategy serviceProviderAccountStrategy) {
        this.serviceProviderAccountStrategy = serviceProviderAccountStrategy;
    }

    public void setLocaleBean(LocaleBean localeBean) {
        this.localeBean = localeBean;
    }

    protected abstract Long getAttributeId();
}
