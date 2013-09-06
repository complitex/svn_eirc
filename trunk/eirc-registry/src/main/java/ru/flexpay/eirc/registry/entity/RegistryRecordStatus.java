package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.FixedIdTypeHandler;
import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@FixedIdTypeHandler
public enum RegistryRecordStatus implements IFixedIdType {

    LOADED(1L),
    PROCESSED_WITH_ERROR(2L),
    FIXED(3L),
    PROCESSED(4L),
    ;

    private static final String RESOURCE_BUNDLE = RegistryRecordStatus.class.getName();

    private Long id;

    private RegistryRecordStatus(Long id) {
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
