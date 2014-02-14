package ru.flexpay.eirc.registry.service.handle.exchange;

import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.service_provider_account.entity.SaldoOut;
import ru.flexpay.eirc.service_provider_account.service.FinancialAttributeBean;
import ru.flexpay.eirc.service_provider_account.service.SaldoOutBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class SaldoOutOperation extends BaseFinancialOperation<SaldoOut> {

    @EJB
    private SaldoOutBean saldoOutBean;

    @Override
    protected FinancialAttributeBean<SaldoOut> getBean() {
        return saldoOutBean;
    }

    @Override
    public Long getCode() {
        return ContainerType.SALDO_OUT.getId();
    }
}
