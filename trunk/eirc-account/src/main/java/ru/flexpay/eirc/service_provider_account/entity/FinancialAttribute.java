package ru.flexpay.eirc.service_provider_account.entity;

import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Pavel Sknar
 */
public abstract class FinancialAttribute extends DictionaryObject {

    private ServiceProviderAccount serviceProviderAccount;

    private BigDecimal amount;

    private Date dateFormation;

    protected FinancialAttribute() {
    }

    protected FinancialAttribute(ServiceProviderAccount serviceProviderAccount) {
        this.serviceProviderAccount = serviceProviderAccount;
    }

    public ServiceProviderAccount getServiceProviderAccount() {
        return serviceProviderAccount;
    }

    public void setServiceProviderAccount(ServiceProviderAccount serviceProviderAccount) {
        this.serviceProviderAccount = serviceProviderAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getDateFormation() {
        return dateFormation;
    }

    public void setDateFormation(Date dateFormation) {
        this.dateFormation = dateFormation;
    }
}
