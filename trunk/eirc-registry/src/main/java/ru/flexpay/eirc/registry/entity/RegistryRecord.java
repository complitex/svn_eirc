package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.complitex.address.entity.AddressEntity;
import org.complitex.correction.entity.LinkStatus;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.DictionaryObject;
import ru.flexpay.eirc.dictionary.entity.Person;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class RegistryRecord extends DictionaryObject implements RegistryRecordData {

    private String serviceCode;
    // лиц. счёт поставщика услуг
    private String personalAccountExt;
    private String cityType;
    private String city;
    private String streetType;
    private String streetCode;
    private String street;
    private String buildingNumber;
    private String buildingCorp;
    private String apartment;
    private String room;
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
    private Long roomId;

    private RegistryRecordStatus status;

    private List<Container> containers = Lists.newArrayList();

    private ImportErrorType importErrorType;

    private Long registryId;

    public RegistryRecord() {
    }

    public RegistryRecord(Long registryId) {
        this.registryId = registryId;
    }

    @Override
    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    @Override
    public String getPersonalAccountExt() {
        return personalAccountExt;
    }

    public void setPersonalAccountExt(String personalAccountExt) {
        this.personalAccountExt = personalAccountExt;
    }

    @Override
    public String getCityType() {
        return cityType;
    }

    public void setCityType(String cityType) {
        this.cityType = cityType;
    }

    @Override
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String getStreetType() {
        return streetType;
    }

    public void setStreetType(String streetType) {
        if (StringUtils.startsWith(streetType, "#")) {
            this.streetCode = StringUtils.removeStart(streetType, "#");
        } else {
            this.streetType = streetType;
        }
    }

    @Override
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    @Override
    public String getBuildingCorp() {
        return buildingCorp;
    }

    public void setBuildingCorp(String buildingCorp) {
        this.buildingCorp = buildingCorp;
    }

    @Override
    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
    }

    @Override
    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public Date getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(Date operationDate) {
        this.operationDate = operationDate;
    }

    @Override
    public Long getUniqueOperationNumber() {
        return uniqueOperationNumber;
    }

    public void setUniqueOperationNumber(Long uniqueOperationNumber) {
        this.uniqueOperationNumber = uniqueOperationNumber;
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public RegistryRecordStatus getStatus() {
        return status;
    }

    public void setStatus(RegistryRecordStatus status) {
        this.status = status;
    }

    @Override
    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    @Override
    public ImportErrorType getImportErrorType() {
        return importErrorType;
    }

    public void setImportErrorType(ImportErrorType importErrorType) {
        this.importErrorType = importErrorType;
    }

    @Override
    public Long getRegistryId() {
        return registryId;
    }

    public void setRegistryId(Long registryId) {
        this.registryId = registryId;
    }

    @Override
    public Long getCityTypeId() {
        return cityTypeId;
    }

    public void setCityTypeId(Long cityTypeId) {
        this.cityTypeId = cityTypeId;
    }

    @Override
    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    @Override
    public Long getStreetTypeId() {
        return streetTypeId;
    }

    public void setStreetTypeId(Long streetTypeId) {
        this.streetTypeId = streetTypeId;
    }

    @Override
    public Long getStreetId() {
        return streetId;
    }

    public void setStreetId(Long streetId) {
        this.streetId = streetId;
    }

    @Override
    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    @Override
    public Long getApartmentId() {
        return apartmentId;
    }

    @Override
    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    @Override
    public Long getRoomId() {
        return roomId;
    }

    @Override
    public void setRoomId(Long id) {
        this.roomId = id;
    }

    @Override
    public void addContainer(Container container) {
        containers.add(container);
    }

    @Override
    public Address getAddress() {
        if (roomId != null) {
            return new Address(roomId, AddressEntity.ROOM);
        } else if (apartmentId != null) {
            return new Address(apartmentId, AddressEntity.APARTMENT);
        } else if (buildingId != null && StringUtils.isEmpty(apartment)) {
            return new Address(buildingId, AddressEntity.BUILDING);
        }
        return null;
    }

    @Override
    public Person getPerson() {
        Person person = new Person();
        person.setLastName(lastName);
        person.setFirstName(firstName);
        person.setMiddleName(middleName);

        return person;
    }

    @Override
    public <T extends LinkStatus> void setStatus(T status) {

        importErrorType = ImportErrorType.getImportErrorType(status);
        RegistryRecordStatus newStatus = RegistryRecordStatus.getRegistryRecordStatus(status);
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    @Override
    public String getStreetCode() {
        return streetCode;
    }

    @Override
    public void writeContainers(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getStreetTypeCode() {
        return null;
    }
}
