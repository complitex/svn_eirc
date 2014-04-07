package ru.flexpay.eirc.service_provider_account.service;

import ru.flexpay.eirc.service_provider_account.entity.CashPayment;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class CashPaymentBean extends PaymentAttributeBean<CashPayment> {
    private static final String NS = CashPaymentBean.class.getName();

    @Override
    public CashPayment getInstance() {
        return new CashPayment();
    }

    @Override
    protected String getNameSpace() {
        return NS;
    }
}
