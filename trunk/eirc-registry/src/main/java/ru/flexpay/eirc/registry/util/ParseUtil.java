package ru.flexpay.eirc.registry.util;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;
import ru.flexpay.eirc.registry.service.parse.RegistryFormatException;
import ru.flexpay.eirc.registry.service.parse.RegistryUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class ParseUtil {

    public static boolean parseContainers(List<Container> distContainers, String containersData) throws RegistryFormatException {

        List<String> containers = StringUtil.splitEscapable(
                containersData, ParseRegistryConstants.CONTAINER_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
        for (String data : containers) {
            if (StringUtils.isBlank(data)) {
                continue;
            }
            if (data.length() > ParseRegistryConstants.MAX_CONTAINER_SIZE) {
                throw new RegistryFormatException(String.format("Too long container found: %s", data));
            }
            List<String> containerData = StringUtil.splitEscapable(
                    data, ParseRegistryConstants.CONTAINER_DATA_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
            if (containerData.size() < 1) {
                throw new RegistryFormatException(String.format("Failed container format: %s", containerData));
            }

            ContainerType containerType = ContainerType.valueOf(Long.parseLong(containerData.get(0)));

            distContainers.add(new Container(data, containerType));
        }
        return true;
    }

    public static boolean fillUpRecord(List<String> messageFieldList, RegistryRecord record) throws RegistryFormatException {
        int n = 1;
        record.setServiceCode(messageFieldList.get(++n));
        record.setPersonalAccountExt(messageFieldList.get(++n));

        /*
        FilterWrapper<Service> filter = FilterWrapper.of(new Service(record.getServiceCode()));
        filter.setSortProperty(null);
        List<Service> services = serviceBean.getServices(filter);
        if (services.size() == 0) {
            processLog.warn("Not found service by code {}", record.getServiceCode());
        }
        */

        // setup consumer address
        String addressStr = messageFieldList.get(++n);
        if (StringUtils.isNotEmpty(addressStr)) {
            List<String> addressFieldList = StringUtil.splitEscapable(
                    addressStr, ParseRegistryConstants.ADDRESS_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);

            if (addressFieldList.size() != 7) {
                throw new RegistryFormatException(
                        String.format("Address group '%s' has invalid number of fields %d",
                                addressStr, addressFieldList.size()));
            }
            record.setCity(addressFieldList.get(0));
            record.setStreetType(addressFieldList.get(1));
            record.setStreet(addressFieldList.get(2));
            record.setBuildingNumber(addressFieldList.get(3));
            record.setBuildingCorp(addressFieldList.get(4));
            record.setApartment(addressFieldList.get(5));
            record.setRoom(addressFieldList.get(6));
        }

        // setup person first, middle, last names
        String fioStr = messageFieldList.get(++n);
        if (StringUtils.isNotEmpty(fioStr)) {
            List<String> fields = RegistryUtil.parseFIO(fioStr);
            record.setLastName(fields.get(0));
            record.setFirstName(fields.get(1));
            record.setMiddleName(fields.get(2));
        }

        // setup ParseRegistryConstants.date
        String operationDate = messageFieldList.get(++n);
        try {
            record.setOperationDate(ParseRegistryConstants.RECORD_DATE_FORMAT.parseDateTime(operationDate).toDate());
        } catch (Exception e) {
            throw e;
        }

        // setup unique operation number
        String uniqueOperationNumberStr = messageFieldList.get(++n);
        if (StringUtils.isNotEmpty(uniqueOperationNumberStr)) {
            record.setUniqueOperationNumber(Long.valueOf(uniqueOperationNumberStr));
        }

        // setup amount
        String amountStr = messageFieldList.get(++n);
        if (StringUtils.isNotEmpty(amountStr)) {
            record.setAmount(new BigDecimal(amountStr));
        }

        // setup containers
        String containersStr = messageFieldList.get(++n);

        return StringUtils.isNotEmpty(containersStr) && parseContainers(record.getContainers(), containersStr);
    }

    public static String fillUpRegistry(List<String> messageFieldList, DateTimeFormatter dateFormat, Registry newRegistry) throws RegistryFormatException {
        int n = 0;
        newRegistry.setRegistryNumber(Long.valueOf(messageFieldList.get(++n)));
        String value = messageFieldList.get(++n);

        Long registryTypeId = Long.valueOf(value);
        RegistryType registryType = null;
        for (RegistryType item : RegistryType.values()) {
            if (item.getId().equals(registryTypeId)) {
                registryType = item;
                break;
            }
        }

        newRegistry.setType(registryType);
        newRegistry.setRecordsCount(Integer.valueOf(messageFieldList.get(++n)));
        newRegistry.setCreationDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
        newRegistry.setFromDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
        newRegistry.setTillDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
        newRegistry.setSenderOrganizationId(Long.valueOf(messageFieldList.get(++n)));
        newRegistry.setRecipientOrganizationId(Long.valueOf(messageFieldList.get(++n)));
        String amountStr = messageFieldList.get(++n);
        if (StringUtils.isNotEmpty(amountStr)) {
            newRegistry.setAmount(new BigDecimal(amountStr));
        }
        if (messageFieldList.size() > n + 1) {
            if (!parseContainers(newRegistry.getContainers(), messageFieldList.get(++n))) {
                return null;
            }
        }
        return value;
    }
}
