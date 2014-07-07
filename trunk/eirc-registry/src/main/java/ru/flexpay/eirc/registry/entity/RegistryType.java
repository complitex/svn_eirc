package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.entity.description.ILocalizedType;
import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public enum RegistryType implements IFixedIdType, ILocalizedType {
    UNKNOWN(0L), 
    SALDO_SIMPLE(1L),
    INCOME(2L), 
    MESSAGE(3L), 
    CLOSED_ACCOUNTS(4L), 
    INFO(5L), 
    CORRECTIONS(6L),
    PAYMENTS(7L),
    CASHLESS_PAYMENTS(8L), 
    REPAYMENT(9L), 
    ERRORS(10L), 
    QUITTANCE(11L), 
    BANK_PAYMENTS(12L);

    private static final String RESOURCE_BUNDLE = RegistryType.class.getName();

    private Long id;

    private RegistryType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public boolean isPayments() {
        return this == PAYMENTS || this == CASHLESS_PAYMENTS;
    }

    public boolean isCashPayments() {
        return this == PAYMENTS;
    }

    public boolean isCashlessPayments() {
        return this == CASHLESS_PAYMENTS;
    }

    @Override
    public String getLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, String.valueOf(getId()), locale);
    }
}
