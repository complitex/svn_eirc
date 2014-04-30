package ru.flexpay.eirc.service_provider_account.entity;

/**
 * @author Pavel Sknar
 */
public class SaldoOut extends FinancialAttribute {
    public SaldoOut() {
    }

    public SaldoOut(ServiceProviderAccount serviceProviderAccount) {
        super(serviceProviderAccount);
    }
}
