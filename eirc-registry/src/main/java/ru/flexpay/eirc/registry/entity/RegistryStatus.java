package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

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
