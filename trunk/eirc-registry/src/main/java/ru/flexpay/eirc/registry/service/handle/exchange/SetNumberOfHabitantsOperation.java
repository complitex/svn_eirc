package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class SetNumberOfHabitantsOperation extends ServiceProviderAccountAttrOperation {

    @Override
    protected Long getAttributeId() {
        return ServiceProviderAccountStrategy.NUMBER_OF_HABITANTS;
    }

    @Override
    public Long getCode() {
        return ContainerType.SET_NUMBER_ON_HABITANTS.getId();
    }
}
