package ru.flexpay.eirc.registry.entity;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.complitex.dictionary.entity.IComponentConfig;
import org.complitex.dictionary.web.component.type.InputPanel;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
import ru.flexpay.eirc.organization_type.entity.OrganizationType;

/**
 * @author Pavel Sknar
 */
public enum RegistryConfig implements IComponentConfig {

    SELF_ORGANIZATION_ID("-1", "general"),

    TMP_DIR("/tmp", "import"),
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

    @Override
    public WebMarkupContainer getComponent(String id, IModel<String> model) {
        if (this.equals(SELF_ORGANIZATION_ID)) {
            return new OrganizationPicker(id, model, OrganizationType.USER_ORGANIZATION.getId());
        } else {
            return new InputPanel<>("config", model, String.class, false, null, true, 40);
        }
    }
}
