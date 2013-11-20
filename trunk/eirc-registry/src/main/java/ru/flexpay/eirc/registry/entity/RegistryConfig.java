package ru.flexpay.eirc.registry.entity;

import org.complitex.dictionary.entity.IConfig;

/**
 * @author Pavel Sknar
 */
public enum  RegistryConfig implements IConfig {

    SELF_ORGANIZATION_ID("-1", "general"),

    NUMBER_FLUSH_REGISTRY_RECORDS("10000", "import"),
    NUMBER_READ_CHARS("32000", "import");

    private String defaultValue;
    private String groupKey;

    RegistryConfig(String defaultValue, String groupKey) {
        this.defaultValue = defaultValue;
        this.groupKey = groupKey;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getGroupKey() {
        return groupKey;
    }
}
