package ru.flexpay.eirc.registry.entity;

import org.complitex.correction.entity.AddressLinkStatus;
import org.complitex.correction.entity.LinkStatus;
import org.complitex.dictionary.entity.description.ILocalizedType;
import org.complitex.dictionary.mybatis.FixedIdTypeHandler;
import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@FixedIdTypeHandler
public enum RegistryRecordStatus implements IFixedIdType, ILocalizedType {

    LOADED_WITH_ERROR(1L),
    LOADED(2L),
    LINKED_WITH_ERROR(3L),
    LINKED(4L),
    PROCESSED_WITH_ERROR(5L),
    PROCESSED(6L),
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

    @Override
    public String getLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, String.valueOf(getId()), locale);
    }

    public static RegistryRecordStatus getRegistryRecordStatus(LinkStatus linkStatus) {
        return linkStatus instanceof AddressLinkStatus && linkStatus != AddressLinkStatus.ADDRESS_LINKED ? LINKED_WITH_ERROR : null;
    }
}
