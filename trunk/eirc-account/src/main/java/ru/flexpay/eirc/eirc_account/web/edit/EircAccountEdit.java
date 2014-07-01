package ru.flexpay.eirc.eirc_account.web.edit;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.address.entity.AddressEntity;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.strategy.StrategyFactory;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.search.CollapsibleInputSearchComponent;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleInputSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import org.odlabs.wiquery.ui.accordion.Accordion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.eirc_account.web.list.EircAccountList;
import ru.flexpay.eirc.service_provider_account.web.component.ServiceProviderAccountListPanel;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircAccountEdit extends TemplatePage {

    @EJB
    private StrategyFactory strategyFactory;

    @EJB
    private EircAccountBean eircAccountBean;

    private SearchComponentState componentState;

    private EircAccount eircAccount;

    private static final List<String> addressDescription = ImmutableList.of("street", "city", "region", "country");


    private final Logger log = LoggerFactory.getLogger(EircAccountEdit.class);

    public EircAccountEdit() {
        init();
    }

    public EircAccountEdit(PageParameters parameters) {
        StringValue eircAccountId = parameters.get("eircAccountId");
        if (eircAccountId != null && !eircAccountId.isNull()) {
            eircAccount = eircAccountBean.getEircAccount(eircAccountId.toLong());
            if (eircAccount == null) {
                throw new RuntimeException("EircAccount by id='" + eircAccountId + "' not found");
            }
        }
        init();
    }

    private void init() {

        if (eircAccount == null) {
            eircAccount = new EircAccount();
        }
        if (eircAccount.getPerson() == null) {
            eircAccount.setPerson(new Person());
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
        if (eircAccount.getId() == null) {
            componentState = (SearchComponentState)(getTemplateSession().getGlobalSearchComponentState().clone());
        } else {
            componentState = new SearchComponentState();
            initSearchComponentState(componentState);
        }

        CollapsibleInputSearchComponent searchComponent = new CollapsibleInputSearchComponent("searchComponent",
                componentState, eircAccountBean.getSearchFilters(), null, ShowMode.ACTIVE, true);
        form.add(searchComponent);

        //eirc account field
        form.add(new TextField<>("accountNumber", new PropertyModel<String>(eircAccount, "accountNumber")).setRequired(true));

        // FIO fields
        form.add(new TextField<>("lastName",   new PropertyModel<String>(eircAccount.getPerson(), "lastName")));
        form.add(new TextField<>("firstName",  new PropertyModel<String>(eircAccount.getPerson(), "firstName")));
        form.add(new TextField<>("middleName", new PropertyModel<String>(eircAccount.getPerson(), "middleName")));

        // service provider accounts
        final Accordion accordion = new Accordion("accordionSPA");
        final Integer active = (Integer)getSession().getAttribute("service_provider_accounts_accordion_active");
        if (active != null && active < 0) {
            accordion.setActive(active);
        } else {
            accordion.setActive(false);
        }
        accordion.setCollapsible(true);
        accordion.setOutputMarkupPlaceholderTag(true);
        accordion.add(new FormComponent<Boolean>("accordionSPAHeader", new Model<>(Boolean.TRUE)) {

        }.add(new AjaxEventBehavior("click") {
            Integer state = accordion.getActive();

            protected void onEvent(AjaxRequestTarget target) {
                synchronized (accordion) {
                    state = state >= 0 ? -1 : 0;
                    getSession().setAttribute("service_provider_accounts_accordion_active", state);
                }
            }
        }));
        form.add(accordion);
        accordion.add(new ServiceProviderAccountListPanel("serviceProviderAccounts", eircAccount.getId() == null? 0: eircAccount.getId(), eircAccount.getEndDate() == null).
                setVisible(eircAccount.getId() != null && eircAccount.getId() > 0));

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

                if (eircAccountBean.eircAccountExists(eircAccount.getId(), address)) {
                    error(getString("error_eirc_account_by_address_exists"));
                    target.add(messages);
                    return;
                }

                if (eircAccountBean.eircAccountExists(eircAccount.getId(), eircAccount.getAccountNumber())) {
                    error(getString("error_eirc_account_exists"));
                    target.add(messages);
                    return;
                }

                eircAccount.setAddress(address);
                eircAccountBean.save(eircAccount);

                getSession().info(getString("saved"));

                setResponsePage(EircAccountList.class);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(messages);
            }
        };
        save.setVisible(eircAccount.getEndDate() == null);
        form.add(save);

        // cancel button
        Link<String> cancel = new Link<String>("cancel") {

            @Override
            public void onClick() {
                setResponsePage(EircAccountList.class);
            }
        };
        form.add(cancel);
    }

    private boolean isNullAddressInput(DomainObject addressInput) {
        return addressInput == null || addressInput.getId() == -1;
    }

    private void initSearchComponentState(SearchComponentState componentState) {
        componentState.clear();

        DomainObject room = null;
        DomainObject apartment = null;
        DomainObject building = null;

        switch (eircAccount.getAddress().getEntity()) {
            case ROOM:
                room = findObject(eircAccount.getAddress().getId(), "room");
                componentState.put("room", room);
            case APARTMENT:
                Long apartmentId = null;
                if (room != null && room.getParentEntityId() == 100) {
                    apartmentId = room.getParentId();
                } else if (room == null) {
                    apartmentId = eircAccount.getAddress().getId();
                }
                if (apartmentId != null) {
                    apartment = findObject(apartmentId, "apartment");
                    componentState.put("apartment", apartment);
                }
            case BUILDING:
                Long buildId = null;
                if (apartment != null) {
                    buildId = apartment.getParentId();
                } else if (room != null) {
                    buildId = room.getParentId();
                } else {
                    buildId = eircAccount.getAddress().getId();
                }

                building = findObject(buildId, "building");
                componentState.put("building", building);
                break;
        }

        if (building == null) {
            throw new RuntimeException("Failed EIRC Account`s address");
        }

        DomainObject child = findObject(building.getParentId(), "building_address");

        for (String desc : addressDescription) {
            child = findObject(child.getParentId(), desc);
            componentState.put(desc, child);
        }
    }

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {
        return ImmutableList.of(new CollapsibleInputSearchToolbarButton(id));
    }

    private DomainObject findObject(Long objectId, String entity) {
        IStrategy strategy = strategyFactory.getStrategy(entity);
        return strategy.findById(objectId, true);
    }
}
