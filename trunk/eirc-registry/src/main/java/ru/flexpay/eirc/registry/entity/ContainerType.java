package ru.flexpay.eirc.registry.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.complitex.dictionary.mybatis.IFixedIdType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static ru.flexpay.eirc.registry.entity.RegistryType.*;

/**
 * @author Pavel Sknar
 */
public enum ContainerType implements IFixedIdType {
    UNDEFINED(0L, null),
    OPEN_ACCOUNT(1L, INFO, SALDO_SIMPLE),
    CLOSE_ACCOUNT(2L, CLOSED_ACCOUNTS),
    SET_RESPONSIBLE_PERSON(3L, INFO, SALDO_SIMPLE),
    SET_NUMBER_ON_HABITANTS(4L, INFO, SALDO_SIMPLE),
    SET_TOTAL_SQUARE(5L, INFO),
    SET_LIVE_SQUARE(6L, INFO),
    SET_WARM_SQUARE(7L, INFO, SALDO_SIMPLE),
    SET_PRIVILEGE_TYPE(8L, INFO),
    SET_PRIVILEGE_OWNER(9L, INFO),
    SET_PRIVILEGE_PERSON(10L, INFO),
    SET_PRIVILEGE_APPROVAL_DOCUMENT(11L, INFO),
    SET_PRIVILEGE_PERSONS_NUMBER(12L, INFO),
    OPEN_SUBACCOUNT(14L, INFO),
    EXTERNAL_ORGANIZATION_ACCOUNT(15L, INFO, SALDO_SIMPLE),

    CASH_PAYMENT(50L, PAYMENTS),
    CASHLESS_PAYMENT(51L, PAYMENTS),
    BANK_PAYMENT(52L, BANK_PAYMENTS),

    BASE(100L, QUITTANCE),
    CHARGE(101L, SALDO_SIMPLE),
    SALDO_OUT(102L, SALDO_SIMPLE),

    ADDRESS_CORRECTION(150L, CORRECTIONS),


    SETUP_PAYMENT_POINT(500L, null),
    REGISTRY_ANNOTATION(501L, null),
    SYNC_IDENTIFIER(502L, null),
    OBJECT_IDENTIFIER(503L, null),
    DETAILS_PAYMENTS_DOCUMENT(504L, PAYMENTS),

    SET_CALCULATION_NUMBER_TENANTS(600L, INFO),
    SET_CALCULATION_NUMBER_REGISTERED(601L, INFO),
    SET_CALCULATION_TOTAL_SQUARE(602L, INFO),
    SET_CALCULATION_LIVE_SQUARE(603L, INFO),
    SET_CALCULATION_HEATING_SQUARE(604L, INFO)
    ;

    private static final Map<Long, ContainerType> CONTAINER_TYPE_MAP;

    static {
        ImmutableMap.Builder<Long, ContainerType> builder = ImmutableMap.builder();

        for (ContainerType containerType : ContainerType.values()) {
            builder.put(containerType.getId(), containerType);
        }

        CONTAINER_TYPE_MAP = builder.build();
    }

    private Long id;
    private Set<RegistryType> registryTypes;

    private ContainerType(Long id, RegistryType... registryTypes) {
        this.id = id;
        this.registryTypes = registryTypes == null? Collections.<RegistryType>emptySet(): Sets.newHashSet(registryTypes);
    }

    @Override
    public Long getId() {
        return id;
    }

    public Set<RegistryType> getRegistryTypes() {
        return registryTypes;
    }

    public boolean isSupport(RegistryType registryType) {
        return registryTypes.contains(registryType);
    }

    public static ContainerType valueOf(Long id) {
        ContainerType containerType = CONTAINER_TYPE_MAP.get(id);
        return containerType != null? containerType : UNDEFINED;
    }
}
