package ru.flexpay.eirc.dictionary;

import com.google.common.collect.Maps;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.mybatis.IFixedIdType;

import java.util.Map;

/**
 * @author Pavel Sknar
 */
public enum OrganizationType implements IFixedIdType {

    USER_ORGANIZATION(1L), SERVICE_PROVIDER(2L);

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
