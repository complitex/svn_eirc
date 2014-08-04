package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.util.CloneUtil;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class ServiceProviderAccountAttrOperation extends GeneralAccountOperation {

    @EJB
    private ServiceProviderAccountStrategy serviceProviderAccountStrategy;

    @EJB
    private LocaleBean localeBean;

    @Override
    public void process(Registry registry, RegistryRecordData registryRecord, Container container,
                        List<OperationResult> results) throws AbstractException {

        ServiceProviderAccount serviceProviderAccount = getServiceProviderAccount(registry, registryRecord);

        DomainObject newObject = serviceProviderAccountStrategy.findById(serviceProviderAccount.getId(), true);
        DomainObject oldObject = CloneUtil.cloneObject(newObject);

        Attribute numberOfHabitants = newObject.getAttribute(getAttributeId());

        BaseAccountOperationData data = getContainerData(container);

        numberOfHabitants.setStringValue(data.getNewValue(), localeBean.getSystemLocaleObject().getId());

        serviceProviderAccountStrategy.update(oldObject, newObject, data.getChangeApplyingDate());

        results.add(new OperationResult<>(oldObject, newObject, getCode()));
    }

    protected abstract Long getAttributeId();
}
