package ru.flexpay.eirc.registry.entity;

import org.complitex.correction.entity.AddressLinkData;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public interface RegistryRecordData extends AddressLinkData, Serializable {
    Long getId();

    void setId(Long id);

    String getServiceCode();

    String getPersonalAccountExt();

    String getFirstName();

    String getMiddleName();

    String getLastName();

    Date getOperationDate();

    Long getUniqueOperationNumber();

    BigDecimal getAmount();

    RegistryRecordStatus getStatus();

    List<Container> getContainers();

    ImportErrorType getImportErrorType();

    Long getRegistryId();

    Long getCityTypeId();

    Long getCityId();

    Long getStreetTypeId();

    Long getStreetId();

    Long getBuildingId();

    Long getApartmentId();

    Long getRoomId();

    void addContainer(Container container);

    Address getAddress();

    Person getPerson();

    String getStreetCode();

    void writeContainers(ByteBuffer buffer);
}
