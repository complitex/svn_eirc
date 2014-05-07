package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author Pavel Sknar
 */
public abstract class AjaxCheckBoxPanel extends Panel implements AjaxComponent {

    private AjaxCheckBox checkBox;

    public AjaxCheckBoxPanel(String id, final IModel<Boolean> model) {
        super(id, model);

        checkBox = new AjaxCheckBox("checkbox", model) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                AjaxCheckBoxPanel.this.onUpdate(target);
            }
        };

        add(checkBox);
    }

    public AjaxCheckBox getCheckBox() {
        return checkBox;
    }
}
