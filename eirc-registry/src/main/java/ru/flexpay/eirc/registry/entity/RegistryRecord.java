package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.Lists;
import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class RegistryRecord extends DictionaryObject {

    private String serviceCode;
    // лиц. счёт поставщика услуг
    private String personalAccountExt;
    private String townType;
    private String townName;
    private String streetType;
    private String streetName;
    private String buildingNum;
    private String buildingBulkNum;
    private String apartmentNum;
    private String firstName;
    private String middleName;
    private String lastName;
    private Date operationDate;
    private Long uniqueOperationNumber;
    private BigDecimal amount;

    private RegistryRecordStatus status;

    private List<Container> containers = Lists.newArrayList();

    private ImportErrorType importErrorType;

    private Long registryId;

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getPersonalAccountExt() {
        return personalAccountExt;
    }

    public void setPersonalAccountExt(String personalAccountExt) {
        this.personalAccountExt = personalAccountExt;
    }

    public String getTownType() {
        return townType;
    }

    public void setTownType(String townType) {
        this.townType = townType;
    }

    public String getTownName() {
        return townName;
    }

    public void setTownName(String townName) {
        this.townName = townName;
    }

    public String getStreetType() {
        return streetType;
    }

    public void setStreetType(String streetType) {
        this.streetType = streetType;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getBuildingNum() {
        return buildingNum;
    }

    public void setBuildingNum(String buildingNum) {
        this.buildingNum = buildingNum;
    }

    public String getBuildingBulkNum() {
        return buildingBulkNum;
    }

    public void setBuildingBulkNum(String buildingBulkNum) {
        this.buildingBulkNum = buildingBulkNum;
    }

    public String getApartmentNum() {
        return apartmentNum;
    }

    public void setApartmentNum(String apartmentNum) {
        this.apartmentNum = apartmentNum;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(Date operationDate) {
        this.operationDate = operationDate;
    }

    public Long getUniqueOperationNumber() {
        return uniqueOperationNumber;
    }

    public void setUniqueOperationNumber(Long uniqueOperationNumber) {
        this.uniqueOperationNumber = uniqueOperationNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public RegistryRecordStatus getStatus() {
        return status;
    }

    public void setStatus(RegistryRecordStatus status) {
        this.status = status;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    public ImportErrorType getImportErrorType() {
        return importErrorType;
    }

    public void setImportErrorType(ImportErrorType importErrorType) {
        this.importErrorType = importErrorType;
    }

    public Long getRegistryId() {
        return registryId;
    }

    public void setRegistryId(Long registryId) {
        this.registryId = registryId;
    }
}
