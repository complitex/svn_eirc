package ru.flexpay.eirc.dictionary.entity;

import com.google.common.collect.Maps;
import org.complitex.dictionary.entity.Locale;

import java.util.Map;

/**
 * @author Pavel Sknar
 */
public class DictionaryNamedObject extends DictionaryObject {

    private Map<Locale, String> names = Maps.newHashMap();

    public Map<Locale, String> getNames() {
        return names;
    }

    public void setNames(Map<Locale, String> names) {
        this.names = names;
    }

    public void addName(Locale locale, String name) {
        names.put(locale, name);
    }

    public String getName(Locale locale) {
        return names.get(locale);
    }
}
