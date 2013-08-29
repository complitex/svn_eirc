package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

import static ru.flexpay.eirc.registry.entity.RegistryType.*;

/**
 * @author Pavel Sknar
 */
public enum ContainerType implements IFixedIdType {
    OPEN_ACCOUNT(1L, INFO),
    CLOSE_ACCOUNT(2L, CLOSED_ACCOUNTS),
    SET_RESPONSIBLE_PERSON(3L, INFO),
    SET_NUMBER_ON_HABITANTS(4L, INFO),
    SET_TOTAL_SQUARE(5L, INFO),
    SET_LIVE_SQUARE(6L, INFO),
    SET_WARM_SQUARE(7L, INFO),
    SET_PRIVILEGE_TYPE(8L, INFO),
    SET_PRIVILEGE_OWNER(9L, INFO),
    SET_PRIVILEGE_PERSON(10L, INFO),
    SET_PRIVILEGE_APPROVAL_DOCUMENT(11L, INFO),
    SET_PRIVILEGE_PERSONS_NUMBER(12L, INFO),
    OPEN_SUBACCOUNT(14L, INFO),
    EXTERNAL_ORGANIZATION_ACCOUNT(15L, INFO),

    SIMPLE_PAYMENT(50L, CASH_PAYMENTS),
    BANK_PAYMENT(52L, BANK_PAYMENTS),

    BASE(100L, QUITTANCE),

    ADDRESS_CORRECTION(150L, CORRECTIONS),


    SETUP_PAYMENT_POINT(500L, null),
    REGISTRY_ANNOTATION(501L, null),
    SYNC_IDENTIFIER(502L, null),
    OBJECT_IDENTIFIER(503L, null),

    SET_CALCULATION_NUMBER_TENANTS(600L, INFO),
    SET_CALCULATION_NUMBER_REGISTERED(601L, INFO),
    SET_CALCULATION_TOTAL_SQUARE(602L, INFO),
    SET_CALCULATION_LIVE_SQUARE(603L, INFO),
    SET_CALCULATION_HEATING_SQUARE(604L, INFO)
    ;

    private Long id;
    private RegistryType registryType;

    private ContainerType(Long id, RegistryType registryType) {
        this.id = id;
        this.registryType = registryType;
    }

    @Override
    public Long getId() {
        return id;
    }

    public RegistryType getRegistryType() {
        return registryType;
    }
}
