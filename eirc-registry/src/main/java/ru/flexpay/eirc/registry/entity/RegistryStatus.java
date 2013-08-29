package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum  RegistryStatus implements IFixedIdType {
    LOADING(0L),
    LOADED(1L),
    LOADING_CANCELED(2L),
    LOADED_WITH_ERROR(3L),

    PROCESSING(4L),
    PROCESSING_WITH_ERROR(5L),
    PROCESSED(6L),
    PROCESSED_WITH_ERROR(7L),
    PROCESSING_CANCELED(8L),

    ROLLBACKING(9L),
    ROLLBACKED(10L),

    CREATING(11L),
    CREATED(12L),
    CREATING_CANCELED(13L),

    PROCESSED_IMPORT_CONSUMER(14L),
    PROCESSED_IMPORT_CONSUMER_WITH_ERROR(15L),
    PROCESSING_IMPORT_CONSUMER(16L),
    PROCESSING_IMPORT_CONSUMER_WITH_ERROR(17L),

    START_PROCESSING(18L);

    private Long id;

    private RegistryStatus(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }
}
