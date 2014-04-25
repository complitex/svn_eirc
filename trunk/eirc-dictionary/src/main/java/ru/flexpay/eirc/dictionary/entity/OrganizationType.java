package ru.flexpay.eirc.dictionary.entity;

import org.complitex.dictionary.mybatis.IFixedIdType;

/**
 * @author Pavel Sknar
 */
public enum OrganizationType implements IFixedIdType {

    USER_ORGANIZATION(1L), SERVICE_PROVIDER(2L), PAYMENT_COLLECTOR(3L);

    private DictionaryObject object = new DictionaryObject() {};

    private OrganizationType() {
    }

    private OrganizationType(Long id) {
        object.setId(id);
    }

    @Override
    public Long getId() {
        return object.getId();
    }
}
