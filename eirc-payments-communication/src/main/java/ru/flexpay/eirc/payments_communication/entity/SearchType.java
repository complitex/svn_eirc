package ru.flexpay.eirc.payments_communication.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum SearchType implements IFixedIdType {
    TYPE_ACCOUNT_NUMBER(1L),
    TYPE_QUITTANCE_NUMBER(2L),
    TYPE_APARTMENT_NUMBER(3L),
    TYPE_SERVICE_PROVIDER_ACCOUNT_NUMBER(4L),
    TYPE_ADDRESS(5L),
    TYPE_COMBINED(6L),
    TYPE_ERC_KVP_NUMBER(7L),
    TYPE_ERC_KVP_ADDRESS(8L),
    TYPE_ADDRESS_STR(9L);

    private Long id;

    private SearchType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }
}
