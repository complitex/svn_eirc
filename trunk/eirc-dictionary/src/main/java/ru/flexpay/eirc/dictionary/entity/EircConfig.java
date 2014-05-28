package ru.flexpay.eirc.dictionary.entity;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.complitex.dictionary.entity.IComponentConfig;
import org.complitex.dictionary.web.component.type.InputPanel;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceTypeStrategy;
import ru.flexpay.eirc.dictionary.web.ModuleInstancePicker;

/**
 * @author Pavel Sknar
 */
public enum EircConfig implements IComponentConfig {

    MODULE_ID("-1", "general"),

    IMPORT_FILE_STORAGE_DIR("/var/tmp/data/import", "import"),
    SYNC_DATA_SOURCE("jdbc/eircConnectionRemoteResource", "import"),
    TMP_DIR("/tmp", "import"),
    NUMBER_FLUSH_REGISTRY_RECORDS("10000", "import"),
    NUMBER_READ_CHARS("32000", "import");

    private String defaultValue;
    private String groupKey;

    EircConfig(String defaultValue, String groupKey) {
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

    @Override
    public WebMarkupContainer getComponent(String id, IModel<String> model) {
        if (this.equals(MODULE_ID)) {
            return new ModuleInstancePicker(id, model, true, null, true, ModuleInstanceTypeStrategy.EIRC_TYPE);
        } else {
            return new InputPanel<>("config", model, String.class, false, null, true, 40);
        }
    }
}
