package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.entity.CashlessPayment;
import ru.flexpay.eirc.service_provider_account.service.CashlessPaymentBean;
import ru.flexpay.eirc.service_provider_account.service.PaymentAttributeBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class CashlessPaymentOperation extends BasePaymentOperation<CashlessPayment> {

    @EJB
    private CashlessPaymentBean cashlessPaymentBean;

    @Override
    protected PaymentAttributeBean<CashlessPayment> getBean() {
        return cashlessPaymentBean;
    }

    @Override
    public Long getCode() {
        return ContainerType.CASHLESS_PAYMENT.getId();
    }
}
