package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum RegistryType implements IFixedIdType {
    UNKNOWN(0L), 
    SALDO(1L), 
    INCOME(2L), 
    MESSAGE(3L), 
    CLOSED_ACCOUNTS(4L), 
    INFO(5L), 
    CORRECTIONS(6L),
    CASH_PAYMENTS(7L), 
    CASHLESS_PAYMENTS(8L), 
    REPAYMENT(9L), 
    ERRORS(10L), 
    QUITTANCE(11L), 
    BANK_PAYMENTS(12L);

    private Long id;

    private RegistryType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }
}
