package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.AbstractFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class CheckBoxSelectorFilter extends AbstractFilter implements AjaxComponent {

    private Model<Boolean> model = new Model<>(false);
    private AjaxCheckBox filter;

    /**
     * @param id   component id
     * @param form
     */
    public CheckBoxSelectorFilter(String id, FilterForm<?> form, final List<AjaxCheckBox> checkBoxes) {
        super(id, form);
        //add(new CheckBoxSelector("filter", checkBox));
        add(filter = new AjaxCheckBox("filter", model) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (AjaxCheckBox component : checkBoxes) {
                    component.setModelObject(model.getObject());
                    target.add(component);
                }
            }
        });
    }

    public void setModelObject(boolean value) {
        model.setObject(value);
    }

    public boolean getModelObject() {
        return model.getObject();
    }

    @Override
    public void onUpdate(AjaxRequestTarget target) {
        target.add(filter);
    }
}
