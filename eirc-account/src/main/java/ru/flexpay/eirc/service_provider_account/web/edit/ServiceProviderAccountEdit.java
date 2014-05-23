package ru.flexpay.eirc.service_provider_account.web.edit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.address.entity.AddressEntity;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleInputSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.dictionary.web.CollapsibleInputSearchComponent;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.eirc_account.web.edit.EircAccountEdit;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.web.component.ServicePicker;
import ru.flexpay.eirc.service_provider_account.entity.ServiceNotAllowableException;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.web.list.ServiceProviderAccountList;

import javax.ejb.EJB;
import java.util.Collections;
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

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    private static final List<Long> EMPTY_ALLOWABLE_SERVICES = ImmutableList.of(-1L);

    private SearchComponentState componentState;

    private ServiceProviderAccount serviceProviderAccount;

    private long eircAccountId;

    private boolean revertToEircAccount;

    public ServiceProviderAccountEdit() {
        init();
    }

    public ServiceProviderAccountEdit(PageParameters parameters) {
        StringValue eircAccountIdValue = parameters.get("eircAccountId");
        if (eircAccountIdValue != null && !eircAccountIdValue.isNull()) {
            eircAccountId = eircAccountIdValue.toLong();
        }

        StringValue revertToEircAccountValue = parameters.get("revertToEircAccount");
        if (revertToEircAccountValue != null && !revertToEircAccountValue.isNull()) {
            revertToEircAccount = revertToEircAccountValue.toBoolean();
        }

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
            if (eircAccountId > 0) {
                EircAccount eircAccount = eircAccountBean.getEircAccount(eircAccountId);
                serviceProviderAccount.setEircAccount(eircAccount);
            }
        }

        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        Form form = new Form("form");
        add(form);

        //address component
        if (serviceProviderAccount.getId() == null) {
            componentState = (SearchComponentState)(getTemplateSession().getGlobalSearchComponentState().clone());
        } else {
            componentState = new SearchComponentState();
        }

        CollapsibleInputSearchComponent searchComponent = new CollapsibleInputSearchComponent("searchComponent",
                 componentState, null, ShowMode.ACTIVE, serviceProviderAccount.getEircAccount().getId() == null) {
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
        final FilterWrapper<Service> servicesFilter = FilterWrapper.of(new Service());
        final ServicePicker service = new ServicePicker("service", new PropertyModel<Service>(serviceProviderAccount, "service"), servicesFilter);
        service.setRequired(true);
        service.setOutputMarkupId(true);
        form.add(service);

        // service provider
        IModel<DomainObject> serviceProviderModel = new IModel<DomainObject>() {

            Organization organization = null;

            @Override
            public DomainObject getObject() {
                return organization;
            }

            @Override
            public void setObject(DomainObject domainObject) {
                List<Long> allowableServices;
                if (domainObject != null) {
                    serviceProviderAccount.setOrganizationId(domainObject.getId());
                    organization = organizationStrategy.findById(domainObject.getId(), false);
                    allowableServices = getAllowableServices();
                } else {
                    serviceProviderAccount.setOrganizationId(null);
                    organization = null;
                    allowableServices = EMPTY_ALLOWABLE_SERVICES;
                }
                if (serviceProviderAccount.getService() != null && !allowableServices.contains(serviceProviderAccount.getService().getId())) {
                    serviceProviderAccount.setService(null);
                }
                servicesFilter.getMap().put("ids", allowableServices);
            }

            public List<Long> getAllowableServices() {
                if (organization == null) {
                    return Collections.emptyList();
                }
                List<Attribute> allowableServices = organization.getAttributes(EircOrganizationStrategy.SERVICE);
                if (allowableServices == null || allowableServices.isEmpty()) {
                    return Collections.emptyList();
                }
                List<Long> ids = Lists.newArrayList();
                for (Attribute allowableService : allowableServices) {
                    ids.add(allowableService.getValueId());
                }
                return ids;
            }

            @Override
            public void detach() {

            }
        };
        serviceProviderModel.setObject(new DomainObject(serviceProviderAccount.getOrganizationId()));
        form.add(new OrganizationPicker("serviceProvider", serviceProviderModel, true, null, true, OrganizationType.SERVICE_PROVIDER.getId()) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(service);
            }
        });

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
                    error(getString("failed_address"));
                    target.add(messages);
                    return;
                } else if (address == null) {
                    address = new Address(addressInput.getId(), AddressEntity.BUILDING);
                }

                EircAccount eircAccount = eircAccountBean.getEircAccount(address);
                if (eircAccount == null) {
                    error(getString("eirc_account_not_found_by_address"));
                    target.add(messages);
                    return;
                }

                serviceProviderAccount.setEircAccount(eircAccount);

                try {
                    serviceProviderAccountBean.save(serviceProviderAccount);
                } catch (ServiceNotAllowableException e) {
                    error(getString("eirc_account_service_not_allowable"));
                    target.add(messages);
                    return;
                }

                getSession().info(getString("saved"));

                setResponsePage(getRevertPage(), getRevertPageParams(eircAccount.getId()));
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(messages);
            }
        };
        save.setVisible(serviceProviderAccount.getEndDate() == null);
        form.add(save);

        // cancel button
        Link<String> cancel = new Link<String>("cancel") {

            @Override
            public void onClick() {
                setResponsePage(getRevertPage(), getRevertPageParams(eircAccountId));
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

    private Class<? extends Page> getRevertPage() {
        if (revertToEircAccount) {
            return EircAccountEdit.class;
        }
        return ServiceProviderAccountList.class;
    }

    private PageParameters getRevertPageParams(Long eircAccountId) {
        PageParameters parameters = new PageParameters();
        if (eircAccountId > 0) {
            parameters.add("eircAccountId", eircAccountId);
        }
        return parameters;
    }
}
