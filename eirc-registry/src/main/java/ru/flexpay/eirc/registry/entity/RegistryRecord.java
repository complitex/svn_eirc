package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.Lists;
import org.complitex.correction.entity.AddressLinkData;
import org.complitex.correction.entity.LinkStatus;
import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class RegistryRecord extends DictionaryObject implements AddressLinkData {

    private String serviceCode;
    // лиц. счёт поставщика услуг
    private String personalAccountExt;
    private String cityType;
    private String city;
    private String streetType;
    private String street;
    private String buildingNumber;
    private String buildingCorp;
    private String apartment;
    private String firstName;
    private String middleName;
    private String lastName;
    private Date operationDate;
    private Long uniqueOperationNumber;
    private BigDecimal amount;

    private Long cityTypeId;
    private Long cityId;
    private Long streetTypeId;
    private Long streetId;
    private Long buildingId;
    private Long apartmentId;

    private RegistryRecordStatus status;

    private List<Container> containers = Lists.newArrayList();

    private ImportErrorType importErrorType;

    private Long registryId;

    private Long recipientOrganizationId;
    private Long senderOrganizationId;

    public RegistryRecord() {
    }

    public RegistryRecord(Long registryId) {
        this.registryId = registryId;
    }

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

    public String getCityType() {
        return cityType;
    }

    public void setCityType(String cityType) {
        this.cityType = cityType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreetType() {
        return streetType;
    }

    public void setStreetType(String streetType) {
        this.streetType = streetType;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getBuildingCorp() {
        return buildingCorp;
    }

    public void setBuildingCorp(String buildingCorp) {
        this.buildingCorp = buildingCorp;
    }

    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
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

    public Long getCityTypeId() {
        return cityTypeId;
    }

    public void setCityTypeId(Long cityTypeId) {
        this.cityTypeId = cityTypeId;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public Long getStreetTypeId() {
        return streetTypeId;
    }

    public void setStreetTypeId(Long streetTypeId) {
        this.streetTypeId = streetTypeId;
    }

    public Long getStreetId() {
        return streetId;
    }

    public void setStreetId(Long streetId) {
        this.streetId = streetId;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    public void setSenderOrganizationId(Long senderOrganizationId) {
        this.senderOrganizationId = senderOrganizationId;
    }

    @Override
    public Long getOrganizationId() {
        return senderOrganizationId;
    }

    public void setRecipientOrganizationId(Long recipientOrganizationId) {
        this.recipientOrganizationId = recipientOrganizationId;
    }

    @Override
    public Long getUserOrganizationId() {
        return recipientOrganizationId;
    }

    @Override
    public <T extends LinkStatus> void setStatus(T status) {

        importErrorType = ImportErrorType.getImportErrorType(status);
        this.status = RegistryRecordStatus.getRegistryRecordStatus(status);
    }

    @Override
    public String getStreetCode() {
        return null;
    }
}
