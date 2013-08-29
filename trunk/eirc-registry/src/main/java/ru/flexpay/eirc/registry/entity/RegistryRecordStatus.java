package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum RegistryRecordStatus implements IFixedIdType {

    LOADED(1L),
    PROCESSED_WITH_ERROR(2L),
    FIXED(3L),
    PROCESSED(4L),
    ;

    private Long id;

    private RegistryRecordStatus(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }
}
