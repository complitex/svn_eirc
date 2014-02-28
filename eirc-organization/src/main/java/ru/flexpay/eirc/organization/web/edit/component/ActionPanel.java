package ru.flexpay.eirc.organization.web.edit.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author Pavel Sknar
 */
public abstract class ActionPanel extends Panel {
    public ActionPanel(String id, IModel<String> model) {
        super(id);
        AjaxLink<String> link = new AjaxLink<String>("actionLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doAction(target);
            }
        };
        link.add(new Label("actionMessage", model));
        add(link);
    }

    abstract protected void doAction(AjaxRequestTarget target);
}
