package ru.flexpay.eirc.payments_communication.entity;

import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

/**
 * @author Pavel Sknar
 */
@XmlType(name="serviceDetails")
public class ServiceDetails extends PersonalInfo {
    private Long serviceId;
    private String serviceName;
    private BigDecimal incomingBalance;
    private BigDecimal outgoingBalance;
    private BigDecimal amount;
    private BigDecimal expence;
    private BigDecimal rate;
    private BigDecimal recalculation;
    private BigDecimal benifit;
    private BigDecimal subsidy;
    private BigDecimal payment;
    private BigDecimal payed;
    private String serviceCode;
    private String serviceProviderAccount;
    private String eircAccount;

    private String serviceMasterIndex;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getIncomingBalance() {
        return incomingBalance;
    }

    public void setIncomingBalance(BigDecimal incomingBalance) {
        this.incomingBalance = incomingBalance;
    }

    public BigDecimal getOutgoingBalance() {
        return outgoingBalance;
    }

    public void setOutgoingBalance(BigDecimal outgoingBalance) {
        this.outgoingBalance = outgoingBalance;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getExpence() {
        return expence;
    }

    public void setExpence(BigDecimal expence) {
        this.expence = expence;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getRecalculation() {
        return recalculation;
    }

    public void setRecalculation(BigDecimal recalculation) {
        this.recalculation = recalculation;
    }

    public BigDecimal getBenifit() {
        return benifit;
    }

    public void setBenifit(BigDecimal benifit) {
        this.benifit = benifit;
    }

    public BigDecimal getSubsidy() {
        return subsidy;
    }

    public void setSubsidy(BigDecimal subsidy) {
        this.subsidy = subsidy;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(BigDecimal payment) {
        this.payment = payment;
    }

    public BigDecimal getPayed() {
        return payed;
    }

    public void setPayed(BigDecimal payed) {
        this.payed = payed;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceProviderAccount() {
        return serviceProviderAccount;
    }

    public void setServiceProviderAccount(String serviceProviderAccount) {
        this.serviceProviderAccount = serviceProviderAccount;
    }

    public String getEircAccount() {
        return eircAccount;
    }

    public void setEircAccount(String eircAccount) {
        this.eircAccount = eircAccount;
    }

    public String getServiceMasterIndex() {
        return serviceMasterIndex;
    }

    public void setServiceMasterIndex(String serviceMasterIndex) {
        this.serviceMasterIndex = serviceMasterIndex;
    }
}
