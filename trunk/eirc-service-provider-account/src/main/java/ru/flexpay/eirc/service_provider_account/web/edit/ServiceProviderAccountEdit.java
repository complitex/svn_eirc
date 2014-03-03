package ru.flexpay.eirc.service_provider_account.web.edit;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.address.entity.AddressEntity;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleInputSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.dictionary.web.CollapsibleInputSearchComponent;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceNotAllowableException;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.web.list.ServiceProviderAccountList;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceProviderAccountEdit extends FormTemplatePage {

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private EircAccountBean eircAccountBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private LocaleBean localeBean;

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    private SearchComponentState componentState;

    private ServiceProviderAccount serviceProviderAccount;

    public ServiceProviderAccountEdit() {
        init();
    }

    public ServiceProviderAccountEdit(PageParameters parameters) {
        StringValue serviceProviderAccountId = parameters.get("serviceProviderAccountId");
        if (serviceProviderAccountId != null && !serviceProviderAccountId.isNull()) {
            serviceProviderAccount = serviceProviderAccountBean.getServiceProviderAccount(serviceProviderAccountId.toLong());
            if (serviceProviderAccount == null) {
                throw new RuntimeException("ServiceProviderAccount by id='" + serviceProviderAccountId + "' not found");
            }
        }
        init();
    }

    private void init() {

        if (serviceProviderAccount == null) {
            serviceProviderAccount = new ServiceProviderAccount();
        }
        if (serviceProviderAccount.getPerson() == null) {
            serviceProviderAccount.setPerson(new Person());
        }
        if (serviceProviderAccount.getEircAccount() == null) {
            serviceProviderAccount.setEircAccount(new EircAccount());
        }

        final Locale locale = localeBean.convert(getLocale());

        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);
        container.add(messages);

        Form form = new Form("form");
        add(form);

        //address component
        if (serviceProviderAccount.getId() == null) {
            componentState = (SearchComponentState)(getTemplateSession().getGlobalSearchComponentState().clone());
        } else {
            componentState = new SearchComponentState();
        }

        CollapsibleInputSearchComponent searchComponent = new CollapsibleInputSearchComponent("searchComponent",
                 componentState, null, ShowMode.ACTIVE, true) {
            @Override
            protected Address getAddress() {
                return serviceProviderAccount.getEircAccount().getAddress();
            }
        };
        form.add(searchComponent);

        //eirc account field
        form.add(new TextField<>("accountNumber", new PropertyModel<String>(serviceProviderAccount, "accountNumber")).setRequired(true));

        // FIO fields
        form.add(new TextField<>("lastName",   new PropertyModel<String>(serviceProviderAccount.getPerson(), "lastName")));
        form.add(new TextField<>("firstName",  new PropertyModel<String>(serviceProviderAccount.getPerson(), "firstName")));
        form.add(new TextField<>("middleName", new PropertyModel<String>(serviceProviderAccount.getPerson(), "middleName")));

        // service
        form.add(new DropDownChoice<>("service",
                new PropertyModel<Service>(serviceProviderAccount, "service"),
                serviceBean.getServices(null),
                new IChoiceRenderer<Service>() {
                    @Override
                    public Object getDisplayValue(Service service) {
                        return service.getName(locale) + " (" + service.getCode() + ")";
                    }

                    @Override
                    public String getIdValue(Service service, int i) {
                        return service.getId().toString();
                    }
                }).setRequired(true));

        // service provider
        form.add(new DropDownChoice<>("serviceProvider",
                new IModel<DomainObject>() {

                    @Override
                    public DomainObject getObject() {
                        return serviceProviderAccount.getOrganizationId() != null ?
                                organizationStrategy.findById(serviceProviderAccount.getOrganizationId(), false) :
                                null;
                    }

                    @Override
                    public void setObject(DomainObject domainObject) {
                        if (domainObject != null) {
                            serviceProviderAccount.setOrganizationId(domainObject.getId());
                        } else {
                            serviceProviderAccount.setOrganizationId(null);
                        }
                    }

                    @Override
                    public void detach() {

                    }
                },
                organizationStrategy.getAllServiceProviders(getLocale()),
                new IChoiceRenderer<DomainObject>() {
                    @Override
                    public Object getDisplayValue(DomainObject serviceProvider) {
                        return organizationStrategy.displayDomainObject(serviceProvider, getLocale());
                    }

                    @Override
                    public String getIdValue(DomainObject serviceProvider, int i) {
                        return serviceProvider.getId().toString();
                    }
                }).setRequired(true));

        // save button
        AjaxButton save = new AjaxButton("save") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                Address address = null;
                DomainObject addressInput = componentState.get("room");

                if (isNullAddressInput(addressInput)) {
                    addressInput = componentState.get("apartment");
                } else {
                    address = new Address(addressInput.getId(), AddressEntity.ROOM);
                }

                if (isNullAddressInput(addressInput)) {
                    addressInput = componentState.get("building");
                } else if (address == null) {
                    address = new Address(addressInput.getId(), AddressEntity.APARTMENT);
                }

                if (isNullAddressInput(addressInput)) {
                    container.error(getString("failed_address"));
                    target.add(container);
                    return;
                } else if (address == null) {
                    address = new Address(addressInput.getId(), AddressEntity.BUILDING);
                }

                EircAccount eircAccount = eircAccountBean.getEircAccount(address);
                if (eircAccount == null) {
                    container.error(getString("eirc_account_not_found_by_address"));
                    target.add(container);
                    return;
                }

                serviceProviderAccount.setEircAccount(eircAccount);

                try {
                    serviceProviderAccountBean.save(serviceProviderAccount);
                } catch (ServiceNotAllowableException e) {
                    container.error(getString("eirc_account_service_not_allowable"));
                    target.add(container);
                    return;
                }

                getSession().info(getString("saved"));

                setResponsePage(ServiceProviderAccountList.class);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(container);
            }
        };
        form.add(save);

        // cancel button
        Link<String> cancel = new Link<String>("cancel") {

            @Override
            public void onClick() {
                setResponsePage(ServiceProviderAccountList.class);
            }
        };
        form.add(cancel);
    }

    private boolean isNullAddressInput(DomainObject addressInput) {
        return addressInput == null || addressInput.getId() == -1;
    }

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {
        return ImmutableList.of(new CollapsibleInputSearchToolbarButton(id));
    }
}
