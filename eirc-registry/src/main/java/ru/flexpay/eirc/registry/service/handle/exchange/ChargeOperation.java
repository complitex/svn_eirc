package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.entity.Charge;
import ru.flexpay.eirc.service_provider_account.service.ChargeBean;
import ru.flexpay.eirc.service_provider_account.service.FinancialAttributeBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ChargeOperation extends BaseFinancialOperation<Charge> {

    @EJB
    private ChargeBean chargeBean;

    @Override
    protected FinancialAttributeBean<Charge> getBean() {
        return chargeBean;
    }

    @Override
    public Long getCode() {
        return ContainerType.CHARGE.getId();
    }
}
