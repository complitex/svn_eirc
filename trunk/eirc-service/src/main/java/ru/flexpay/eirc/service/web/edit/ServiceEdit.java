package ru.flexpay.eirc.service.web.edit;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.web.list.ServiceList;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceEdit extends FormTemplatePage {

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private LocaleBean localeBean;

    private Service service;
    private Service parentService;

    public ServiceEdit() {
        init();
    }

    public ServiceEdit(PageParameters parameters) {
        StringValue serviceId = parameters.get("serviceId");
        if (serviceId != null && !serviceId.isNull()) {
            service = serviceBean.getService(serviceId.toLong());
            if (service == null) {
                throw new RuntimeException("Service by id='" + serviceId + "' not found");
            } else if (service.getParentId() != null) {
                parentService = serviceBean.getService(service.getParentId());
            }
        }
        init();
    }

    private void init() {

        if (service == null) {
            service = new Service();
        }

        final Locale locale = localeBean.convert(getLocale());

        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        Form<Service> form = new Form<>("form", new CompoundPropertyModel<>(service));
        add(form);

        // Service code field
        form.add(new TextField<>("code"));

        // Russian service name
        form.add(new TextField<>("nameRu").setRequired(true));

        // Ukrainian service name
        form.add(new TextField<>("nameUk"));

        // Parent service
        List<Service> services = serviceBean.getServices(FilterWrapper.of(new Service()));
        if (service.getId() != null) {
            services.remove(service);
        }
        form.add(new DropDownChoice<>("parent", new Model<Service>() {
            @Override
            public Service getObject() {
                return parentService;
            }

            @Override
            public void setObject(Service object) {
                parentService = object;
            }
        }, services, new IChoiceRenderer<Service>() {
            @Override
            public Object getDisplayValue(Service object) {
                return object != null? object.getName(locale) : "";
            }

            @Override
            public String getIdValue(Service object, int index) {
                return object != null? object.getId().toString(): "-1";
            }
        }).setNullValid(true));

        // save button
        AjaxButton save = new AjaxButton("save") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                if (parentService != null) {
                    service.setParentId(parentService.getId());
                } else {
                    service.setParentId(null);
                }

                serviceBean.save(service);

                getSession().info(getString("saved"));

                setResponsePage(ServiceList.class);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(messages);
            }
        };
        form.add(save);

        // cancel button
        Link<String> cancel = new Link<String>("cancel") {

            @Override
            public void onClick() {
                setResponsePage(ServiceList.class);
            }
        };
        form.add(cancel);
    }
}
