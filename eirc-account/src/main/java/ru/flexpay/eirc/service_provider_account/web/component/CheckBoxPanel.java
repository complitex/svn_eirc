package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author Pavel Sknar
 */
public class CheckBoxPanel extends Panel {

    private CheckBox checkBox;

    public CheckBoxPanel(String id, final IModel<Boolean> model) {
        super(id, model);

        checkBox = new CheckBox("checkbox", model);

        add(checkBox);
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }
}
