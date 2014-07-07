package ru.flexpay.eirc.payments_communication.entity;

import org.complitex.dictionary.entity.description.ILocalizedType;
import org.complitex.dictionary.util.ResourceUtil;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public enum ResponseStatus implements ILocalizedType {

    OK(1L),
    INCORRECT_AUTHENTICATION_DATA(8L),
    UNKNOWN_REQUEST(9L),
    QUITTANCE_NOT_FOUND(10L),
    ACCOUNT_NOT_FOUND(11L),
    ADDRESS_NOT_FOUND(12L),
    INVALID_QUITTANCE_NUMBER(13L),
    INTERNAL_ERROR(14L),
    RECIEVE_TIMEOUT(15L),
    SERVICE_NOT_FOUND(16L),

    ;

    private static final String RESOURCE_BUNDLE = ResponseStatus.class.getName();

    private Long id;

    private ResponseStatus(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, String.valueOf(getId()), locale);
    }
}
