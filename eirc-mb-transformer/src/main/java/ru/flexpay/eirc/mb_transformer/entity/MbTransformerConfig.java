package ru.flexpay.eirc.mb_transformer.entity;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.complitex.dictionary.entity.IComponentConfig;
import org.complitex.dictionary.web.component.type.InputPanel;
import org.complitex.dictionary.web.model.LongModel;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.mb_transformer.web.component.OrganizationPicker;

/**
 * @author Pavel Sknar
 */
public enum MbTransformerConfig implements IComponentConfig {

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

    @Override
    public WebMarkupContainer getComponent(String id, IModel<String> model) {
        if (this.equals(EIRC_ORGANIZATION_ID)) {
            return new OrganizationPicker(id, new LongModel(model), OrganizationType.USER_ORGANIZATION.getId());
        } if (this.equals(MB_ORGANIZATION_ID)) {
            return new OrganizationPicker(id, new LongModel(model), OrganizationType.PAYMENT_COLLECTOR.getId());
        } else {
            return new InputPanel<>("config", model, String.class, false, null, true, 40);
        }
    }


}
