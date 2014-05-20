package ru.flexpay.eirc.service.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;

/**
 *
 * @author Artem
 */
public class ServicePicker extends FormComponentPanel<Service> {

    @EJB
    private LocaleBean localeBean;

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new PackageResourceReference(
                ServicePicker.class, ServicePicker.class.getSimpleName() + ".css")));
        response.render(JavaScriptHeaderItem.forReference(new PackageResourceReference(
                ServicePicker.class, ServicePicker.class.getSimpleName() + ".js")));
    }

    public ServicePicker(String id, IModel<Service> model) {
        this(id, model, false, null, true);
    }

    public ServicePicker(String id, IModel<Service> model, FilterWrapper<Service> filter) {
        this(id, model, filter, false, null, true);
    }

    public ServicePicker(String id, IModel<Service> model, boolean required,
                         IModel<String> labelModel, boolean enabled) {
        this(id, model, FilterWrapper.of(new Service()), required, labelModel, enabled);
    }

    public ServicePicker(String id, IModel<Service> model, FilterWrapper<Service> filter, boolean required,
                         IModel<String> labelModel, boolean enabled) {
        super(id, model);

        setRequired(required);
        setLabel(labelModel);

        final Label serviceLabel = new Label("serviceLabel",
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        Service service = getModelObject();
                        if (service != null) {
                            return service.getName(localeBean.convert(getLocale()));
                        } else {
                            return getString("service_not_selected");
                        }
                    }
                });
        serviceLabel.setOutputMarkupId(true);
        add(serviceLabel);

        final ServiceDialog lookupDialog = new ServiceDialog("lookupDialog", model, filter, enabled, serviceLabel);
        add(lookupDialog);

        AjaxLink<Void> choose = new AjaxLink<Void>("choose") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                lookupDialog.open(target);
            }
        };
        choose.setVisibilityAllowed(enabled);
        add(choose);
    }

    @Override
    protected void convertInput() {
        setConvertedInput(getModelObject());
    }
}
