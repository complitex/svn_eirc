package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.model.IModel;

/**
 * @author Pavel Sknar
 */
public abstract class AbstractFilter<T> extends org.apache.wicket.extensions.markup.html.repeater.data.table.filter.AbstractFilter
        implements AjaxComponent {

    private Component filterComponent;

    /**
     * @param id   component id
     * @param form form
     * @param model model
     */
    public AbstractFilter(String id, FilterForm<?> form, IModel<T> model) {
        super(id, form);
        add(filterComponent = createFilterComponent(id, model));
        filterComponent.setOutputMarkupId(true);
    }

    @Override
    public void onUpdate(AjaxRequestTarget target) {
        target.add(filterComponent);
    }

    protected abstract Component createFilterComponent(String id, IModel<T> model);

}
