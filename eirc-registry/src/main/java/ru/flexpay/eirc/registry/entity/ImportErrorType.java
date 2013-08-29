package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum  ImportErrorType implements IFixedIdType {

    ;

    private Long id;

    private ImportErrorType(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }
}
