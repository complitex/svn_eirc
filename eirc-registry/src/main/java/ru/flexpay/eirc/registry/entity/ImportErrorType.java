package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public enum  ImportErrorType implements IFixedIdType {

    FIXED(1L);

    private static final String RESOURCE_BUNDLE = ImportErrorType.class.getName();

    private Long id;

    private ImportErrorType(Long id) {
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
