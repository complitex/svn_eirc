package ru.flexpay.eirc.payments_communication.entity;

import com.google.common.collect.Lists;
import ru.flexpay.eirc.payments_communication.util.DateAdapter;
import ru.flexpay.eirc.payments_communication.util.DateWithTimeAdapter;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@XmlType(name="quittanceInfo")
public class QuittanceInfo extends PersonalInfo {

    private String quittanceNumber;
    private String eircAccount;
    private Integer orderNumber;

    @XmlJavaTypeAdapter(type = Date.class, value = DateWithTimeAdapter.class)
    private Date creationDate;

    private BigDecimal totalPayed;
    private BigDecimal totalToPay;

    @XmlJavaTypeAdapter(type = Date.class, value = DateAdapter.class)
    private Date beginDate;

    @XmlJavaTypeAdapter(type = Date.class, value = DateAdapter.class)
    private Date endDate;

    private List<ServiceDetails> serviceDetails = Lists.newArrayList();

    public String getQuittanceNumber() {
        return quittanceNumber;
    }

    public void setQuittanceNumber(String quittanceNumber) {
        this.quittanceNumber = quittanceNumber;
    }

    public String getEircAccount() {
        return eircAccount;
    }

    public void setEircAccount(String eircAccount) {
        this.eircAccount = eircAccount;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public BigDecimal getTotalPayed() {
        return totalPayed;
    }

    public void setTotalPayed(BigDecimal totalPayed) {
        this.totalPayed = totalPayed;
    }

    public BigDecimal getTotalToPay() {
        return totalToPay;
    }

    public void setTotalToPay(BigDecimal totalToPay) {
        this.totalToPay = totalToPay;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<ServiceDetails> getServiceDetails() {
        return serviceDetails;
    }

    public void setServiceDetails(List<ServiceDetails> serviceDetails) {
        this.serviceDetails = serviceDetails;
    }
}
