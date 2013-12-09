package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class SetLiveSquareOperation extends ServiceProviderAccountAttrOperation {

    @Override
    protected Long getAttributeId() {
        return ServiceProviderAccountStrategy.LIVE_SQUARE;
    }

    @Override
    public Long getCode() {
        return ContainerType.SET_LIVE_SQUARE.getId();
    }
}
