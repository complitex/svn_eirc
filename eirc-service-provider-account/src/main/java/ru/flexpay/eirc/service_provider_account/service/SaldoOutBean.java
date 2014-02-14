package ru.flexpay.eirc.service_provider_account.service;

import ru.flexpay.eirc.service_provider_account.entity.SaldoOut;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class SaldoOutBean extends FinancialAttributeBean<SaldoOut> {
    private static final String NS = SaldoOutBean.class.getName();

    @Override
    public SaldoOut getInstance() {
        return new SaldoOut();
    }

    @Override
    protected String getNameSpace() {
        return NS;
    }
}
