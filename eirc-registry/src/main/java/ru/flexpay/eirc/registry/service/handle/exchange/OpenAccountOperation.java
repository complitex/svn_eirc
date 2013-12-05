package ru.flexpay.eirc.registry.service.handle.exchange;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.complitex.address.strategy.city.CityStrategy;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.AttributeUtil;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class OpenAccountOperation extends Operation<ServiceProviderAccount> {

    private EircAccountBean eircAccountBean;

    private ServiceProviderAccountBean serviceProviderAccountBean;

    private CityStrategy cityStrategy;

    private ServiceBean serviceBean;

    private Address address;
    private Person person;

    private Long cityId;

    private String serviceProviderAccountNumber;
    private Long organizationId;
    private String serviceCode;

    private ServiceProviderAccount serviceProviderAccount;

    /**
     * Parse data and set operation id. Executing {@link ru.flexpay.eirc.registry.service.handle.exchange.Operation#prepareData}
     *
     * @param container Container
     * @throws org.complitex.dictionary.service.exception.AbstractException
     *
     */
    public OpenAccountOperation(Registry registry, RegistryRecord registryRecord, Container container) throws AbstractException {
        super(container);
        address = registryRecord.getAddress();
        person = registryRecord.getPerson();
        cityId = registryRecord.getCityId();
        serviceProviderAccountNumber = registryRecord.getPersonalAccountExt();
        organizationId = registry.getSenderOrganizationId();
        serviceCode = registryRecord.getServiceCode();

        if (address == null) {
            throw new DataNotFoundException("Address empty in registry record: {0}", registryRecord);
        }
    }

    @Override
    public void process() throws AbstractException {
        EircAccount eircAccount = eircAccountBean.getEircAccount(address);
        if (eircAccount == null) {
            DomainObject city = cityStrategy.findById(cityId, true);
            String cityPrefix = AttributeUtil.getStringValue(city, 402L);
            if (cityPrefix == null) {
                throw new DataNotFoundException("Not found EIRC prefix for city '{0}')", cityId);
            }
            String eircAccountNumber;
            try {
                eircAccountNumber = eircAccountBean.generateEircAccountNumber(cityPrefix);
            } catch (CheckDigitException e) {
                throw new ContainerDataException(e, "Failed generate eirc account number");
            }
            if (eircAccountNumber == null) {
                throw new ContainerDataException("Failed generate eirc account number: it is null");
            }
            eircAccount = new EircAccount();
            eircAccount.setAddress(address);
            eircAccount.setAccountNumber(eircAccountNumber);
            eircAccount.setPerson(person);
            eircAccountBean.save(eircAccount);
        }

        //TODO find by external id, if service code started by # (maybe using correction)
        FilterWrapper<Service> filter = FilterWrapper.of(new Service(serviceCode));
        filter.setSortProperty(null);
        List<Service> services = serviceBean.getServices(filter);
        if (services.size() == 0) {
            throw new DataNotFoundException("Not found service by code {0}", serviceCode);
        }

        serviceProviderAccount = new ServiceProviderAccount();
        serviceProviderAccount.setAccountNumber(serviceProviderAccountNumber);
        serviceProviderAccount.setEircAccount(eircAccount);
        serviceProviderAccount.setOrganizationId(organizationId);
        serviceProviderAccount.setService(services.get(0));
        serviceProviderAccount.setPerson(person);
        serviceProviderAccount.setBeginDate(getChangeApplyingDate());

        serviceProviderAccountBean.save(serviceProviderAccount);
    }

    public void setEircAccountBean(EircAccountBean eircAccountBean) {
        this.eircAccountBean = eircAccountBean;
    }

    public void setServiceProviderAccountBean(ServiceProviderAccountBean serviceProviderAccountBean) {
        this.serviceProviderAccountBean = serviceProviderAccountBean;
    }

    public void setCityStrategy(CityStrategy cityStrategy) {
        this.cityStrategy = cityStrategy;
    }

    public void setServiceBean(ServiceBean serviceBean) {
        this.serviceBean = serviceBean;
    }

    @Override
    protected void prepareData(List<String> containerData) throws AbstractException {

    }

    @Override
    public ServiceProviderAccount getOldObject() {
        return null;
    }

    @Override
    public ServiceProviderAccount getNewObject() {
        return serviceProviderAccount;
    }
}
