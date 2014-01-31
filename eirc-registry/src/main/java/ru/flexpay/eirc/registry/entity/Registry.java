package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private Date loadDate;

    private Long senderOrganizationId;
    private Long recipientOrganizationId;
    private BigDecimal amount;

    private ImportErrorType importErrorType;

    private List<Container> containers = Lists.newArrayList();

    private Map<RegistryFileType, RegistryFile> files = Maps.newHashMap();

    public Registry() {
    }

    public Registry(Long id) {
        this.setId(id);
    }

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

    public Date getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = loadDate;
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

    public ImportErrorType getImportErrorType() {
        return importErrorType;
    }

    public void setImportErrorType(ImportErrorType importErrorType) {
        this.importErrorType = importErrorType;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    public Map<RegistryFileType, RegistryFile> getFiles() {
        return files;
    }

    public void setFiles(Map<RegistryFileType, RegistryFile> files) {
        this.files = files;
    }

    /**
     * MyBatis setter.
     *
     * @param registryFile Content key and value. key is type of registry file, value is registry file.
     */
    public void setName(Map<String, Object> registryFile) {
        files.put((RegistryFileType)registryFile.get("registryFileType"), (RegistryFile)registryFile.get("registryFile"));

    }

    public void addContainer(Container container) {
        containers.add(container);
    }

    public Container getContainer(ContainerType containerType) {
        for (Container container : containers) {
            if (container.getType().equals(containerType)) {
                return container;
            }
        }
        return null;
    }
}
