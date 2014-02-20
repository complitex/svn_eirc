package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.entity.CashPayment;
import ru.flexpay.eirc.service_provider_account.service.CashPaymentBean;
import ru.flexpay.eirc.service_provider_account.service.PaymentAttributeBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class CashPaymentOperation extends BasePaymentOperation<CashPayment> {

    @EJB
    private CashPaymentBean cashPaymentBean;

    @Override
    protected PaymentAttributeBean<CashPayment> getBean() {
        return cashPaymentBean;
    }

    @Override
    public Long getCode() {
        return ContainerType.CASH_PAYMENT.getId();
    }
}