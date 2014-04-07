package ru.flexpay.eirc.service_provider_account.service;

import ru.flexpay.eirc.service_provider_account.entity.Charge;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ChargeBean extends FinancialAttributeBean<Charge> {

    private static final String NS = ChargeBean.class.getName();

    @Override
    public Charge getInstance() {
        return new Charge();
    }

    @Override
    protected String getNameSpace() {
        return NS;
    }
}
