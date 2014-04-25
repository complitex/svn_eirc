package ru.flexpay.eirc.payments_communication.entity;

import com.google.common.collect.ImmutableMap;
import org.complitex.dictionary.mybatis.IFixedIdType;

import java.util.Map;

/**
 * @author Pavel Sknar
 */
public enum SearchType implements IFixedIdType {
    UNKNOWN_TYPE(0L),
    TYPE_ACCOUNT_NUMBER(1L),
    TYPE_QUITTANCE_NUMBER(2L),
    TYPE_APARTMENT_NUMBER(3L),
    TYPE_SERVICE_PROVIDER_ACCOUNT_NUMBER(4L),
    TYPE_ADDRESS(5L),
    TYPE_COMBINED(6L),
    TYPE_ERC_KVP_NUMBER(7L),
    TYPE_ERC_KVP_ADDRESS(8L),
    TYPE_ADDRESS_STR(9L),
    TYPE_BUILDING_NUMBER(10L),
    TYPE_ROOM_NUMBER(11L);

    private static final Map<Long, SearchType> SEARCH_TYPES;

    static {
        ImmutableMap.Builder<Long, SearchType> builder = ImmutableMap.builder();
        for (SearchType searchType : SearchType.values()) {
            builder.put(searchType.getId(), searchType);
        }
        SEARCH_TYPES = builder.build();
    }

    private Long id;

    private SearchType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public static SearchType getSearchType(long id) {
        SearchType searchType = SEARCH_TYPES.get(id);
        return searchType == null? UNKNOWN_TYPE : searchType;
    }
}
