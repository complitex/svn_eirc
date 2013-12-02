package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public enum  RegistryStatus implements IFixedIdType {
    LOADING(0L),
    LOADING_WITH_ERROR(1L),
    LOADED(2L),
    LOADING_CANCELED(3L),
    LOADED_WITH_ERROR(4L),

    PROCESSING(5L),
    PROCESSING_WITH_ERROR(6L),
    PROCESSED(7L),
    PROCESSED_WITH_ERROR(8L),
    PROCESSING_CANCELED(9L),

    ROLLBACKING(10L),
    ROLLBACKED(11L),

    LINKING(19L),
    LINKING_WITH_ERROR(20L),
    LINKED(21L),
    LINKED_WITH_ERROR(22L),
    LINKING_CANCELED(23L);

    private static final String RESOURCE_BUNDLE = RegistryStatus.class.getName();

    private Long id;

    private RegistryStatus(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, String.valueOf(getId()), locale);
    }
}
