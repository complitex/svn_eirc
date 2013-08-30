package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.Lists;
import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class Registry extends DictionaryObject {
    private Long registryNumber;
    private RegistryType type;
    private RegistryStatus status;

    private int recordsCount;
    private int errorsCount = -1;

    private Date creationDate;
    private Date fromDate;
    private Date tillDate;

    private Long senderOrganizationId;
    private Long recipientOrganizationId;
    private BigDecimal amount;

    private List<Container> containers = Lists.newArrayList();

    public Long getRegistryNumber() {
        return registryNumber;
    }

    public void setRegistryNumber(Long registryNumber) {
        this.registryNumber = registryNumber;
    }

    public RegistryType getType() {
        return type;
    }

    public void setType(RegistryType type) {
        this.type = type;
    }

    public RegistryStatus getStatus() {
        return status;
    }

    public void setStatus(RegistryStatus status) {
        this.status = status;
    }

    public int getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(int recordsCount) {
        this.recordsCount = recordsCount;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getTillDate() {
        return tillDate;
    }

    public void setTillDate(Date tillDate) {
        this.tillDate = tillDate;
    }

    public Long getSenderOrganizationId() {
        return senderOrganizationId;
    }

    public void setSenderOrganizationId(Long senderOrganizationId) {
        this.senderOrganizationId = senderOrganizationId;
    }

    public Long getRecipientOrganizationId() {
        return recipientOrganizationId;
    }

    public void setRecipientOrganizationId(Long recipientOrganizationId) {
        this.recipientOrganizationId = recipientOrganizationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }
}
