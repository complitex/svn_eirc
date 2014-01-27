package ru.flexpay.eirc.mb_transformer.entity;

import org.complitex.dictionary.entity.IConfig;

/**
 * @author Pavel Sknar
 */
public enum MbTransformerConfig implements IConfig {

    EIRC_ORGANIZATION_ID("-1", "general"),
    MB_ORGANIZATION_ID("-1", "general"),

    WORK_DIR("/var/tmp/data", "general"),
    TMP_DIR("/tmp", "general"),

    EIRC_DATA_SOURCE("jdbc/eircResource", "general");

    private String defaultValue;
    private String groupKey;

    MbTransformerConfig(String defaultValue, String groupKey) {
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
