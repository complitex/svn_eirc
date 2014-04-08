package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Pavel Sknar
 */
public abstract class AjaxGoAndClearFilter extends GoAndClearFilter {

    public AjaxGoAndClearFilter(String id, FilterForm<?> form, final IModel<String> goModel, final IModel<String> clearModel) {
        super(id, form, goModel, clearModel);

        final Button goButton = getGoButton();
        goButton.replaceWith(new AjaxButton(goButton.getId(), goModel) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onGoSubmit(target, form);
            }
        }).add(new AttributeAppender("class", new Model<>(" btnSmall")));
        final Button clearButton = getClearButton();
        clearButton.replaceWith(new AjaxButton(clearButton.getId(), clearModel) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onClearSubmit(target, form);
            }
        }).add(new AttributeAppender("class", new Model<>(" btnSmall")));
    }

    public abstract void onGoSubmit(AjaxRequestTarget target, Form<?> form);

    public abstract void onClearSubmit(AjaxRequestTarget target, Form<?> form);
}
