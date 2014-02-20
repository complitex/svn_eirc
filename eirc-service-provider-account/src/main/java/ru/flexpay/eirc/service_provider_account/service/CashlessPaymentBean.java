package ru.flexpay.eirc.service_provider_account.service;

import ru.flexpay.eirc.service_provider_account.entity.CashlessPayment;

import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class CashlessPaymentBean extends PaymentAttributeBean<CashlessPayment> {
    private static final String NS = CashlessPaymentBean.class.getName();

    @Override
    public CashlessPayment getInstance() {
        return new CashlessPayment();
    }

    @Override
    protected String getNameSpace() {
        return NS;
    }
}

