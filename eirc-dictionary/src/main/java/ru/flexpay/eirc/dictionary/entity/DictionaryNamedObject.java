package ru.flexpay.eirc.dictionary.entity;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.entity.Locale;

import java.util.Map;

/**
 * @author Pavel Sknar
 */
public class DictionaryNamedObject extends DictionaryObject {

    private Long nameId;

    private Map<Locale, String> names = Maps.newHashMap();

    public Long getNameId() {
        return nameId;
    }

    public void setNameId(Long nameId) {
        this.nameId = nameId;
    }

    public Map<Locale, String> getNames() {
        return names;
    }

    public void setNames(Map<Locale, String> names) {
        this.names = names;
    }

    /**
     * MyBatis setter.
     *
     * @param name Content key and value. key is Locale, value is name.
     */
    public void setName(Map<String, Object> name) {
        addName((Locale)name.get("key"), (String)name.get("value"));

    }

    public void addName(Locale locale, String name) {
        names.put(locale, name);
    }

    public String getName(Locale locale) {
        String name = names.get(locale);
        return StringUtils.isNotEmpty(name)? name : "";
    }

    public String getName() {
        return names.size() > 0? names.values().iterator().next() : "";
    }
}
