package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.ImmutableMap;
import org.complitex.correction.entity.AddressLinkStatus;
import org.complitex.correction.entity.LinkStatus;
import org.complitex.dictionary.entity.description.ILocalizedType;
import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
public enum ImportErrorType implements IFixedIdType, ILocalizedType {

    CITY_UNRESOLVED(1L),
    STREET_TYPE_UNRESOLVED(2L),
    STREET_UNRESOLVED(3L),
    STREET_AND_BUILDING_UNRESOLVED(4L),
    BUILDING_UNRESOLVED(5L),
    APARTMENT_UNRESOLVED(6L),
    ROOM_UNRESOLVED(19L),

    /* найдено больше одной записи адреса во внутреннем адресном справочнике */
    MORE_ONE_CITY(7L),
    MORE_ONE_STREET_TYPE(8L),
    MORE_ONE_STREET(9L),
    MORE_ONE_BUILDING(10L),
    MORE_ONE_APARTMENT(11L),
    MORE_ONE_ROOM(20L),

    /* Найдено более одной записи в коррекциях */
    MORE_ONE_CITY_CORRECTION(12L),
    MORE_ONE_STREET_TYPE_CORRECTION(13L),
    MORE_ONE_STREET_CORRECTION(14L),
    MORE_ONE_BUILDING_CORRECTION(15L),
    MORE_ONE_APARTMENT_CORRECTION(16L),
    MORE_ONE_ROOM_CORRECTION(21L),

    ACCOUNT_UNRESOLVED(17L),
    MORE_ONE_ACCOUNT(18L);

    private static final String RESOURCE_BUNDLE = ImportErrorType.class.getName();

    private Long id;
    private static final Map<LinkStatus, ImportErrorType> mapper = ImmutableMap.<LinkStatus, ImportErrorType>builder().
            put(AddressLinkStatus.CITY_UNRESOLVED, CITY_UNRESOLVED).
            put(AddressLinkStatus.STREET_TYPE_UNRESOLVED, STREET_TYPE_UNRESOLVED).
            put(AddressLinkStatus.STREET_UNRESOLVED, STREET_UNRESOLVED).
            put(AddressLinkStatus.STREET_AND_BUILDING_UNRESOLVED, STREET_AND_BUILDING_UNRESOLVED).
            put(AddressLinkStatus.BUILDING_UNRESOLVED, BUILDING_UNRESOLVED).
            put(AddressLinkStatus.APARTMENT_UNRESOLVED, APARTMENT_UNRESOLVED).
            put(AddressLinkStatus.ROOM_UNRESOLVED, ROOM_UNRESOLVED).
            put(AddressLinkStatus.MORE_ONE_CITY, MORE_ONE_CITY).
            put(AddressLinkStatus.MORE_ONE_STREET_TYPE, MORE_ONE_STREET_TYPE).
            put(AddressLinkStatus.MORE_ONE_STREET, MORE_ONE_STREET).
            put(AddressLinkStatus.MORE_ONE_BUILDING, MORE_ONE_BUILDING).
            put(AddressLinkStatus.MORE_ONE_APARTMENT, MORE_ONE_APARTMENT).
            put(AddressLinkStatus.MORE_ONE_ROOM, MORE_ONE_ROOM).
            put(AddressLinkStatus.MORE_ONE_CITY_CORRECTION, MORE_ONE_CITY_CORRECTION).
            put(AddressLinkStatus.MORE_ONE_STREET_TYPE_CORRECTION, MORE_ONE_STREET_TYPE_CORRECTION).
            put(AddressLinkStatus.MORE_ONE_STREET_CORRECTION, MORE_ONE_STREET_CORRECTION).
            put(AddressLinkStatus.MORE_ONE_BUILDING_CORRECTION, MORE_ONE_BUILDING_CORRECTION).
            put(AddressLinkStatus.MORE_ONE_APARTMENT_CORRECTION, MORE_ONE_APARTMENT_CORRECTION).
            put(AddressLinkStatus.MORE_ONE_ROOM_CORRECTION, MORE_ONE_ROOM_CORRECTION).
            build();

    private ImportErrorType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, String.valueOf(getId()), locale);
    }

    public static ImportErrorType getImportErrorType(LinkStatus linkStatus) {
        return mapper.get(linkStatus);
    }
}
