package ru.flexpay.eirc.service_provider_account.web.edit;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.address.entity.AddressEntity;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.strategy.StrategyFactory;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleInputSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
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
    private StrategyFactory strategyFactory;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    private SearchComponentState componentState;

    private ServiceProviderAccount serviceProviderAccount;

    private static final List<String> addressDescription = ImmutableList.of("street", "city", "region", "country");


    private static final Logger log = LoggerFactory.getLogger(ServiceProviderAccountEdit.class);

    public ServiceProviderAccountEdit() {
        init();
    }

    public ServiceProviderAccountEdit(PageParameters parameters) {
        StringValue eircAccountId = parameters.get("eircAccountId");
        if (eircAccountId != null && !eircAccountId.isNull()) {
            serviceProviderAccount = serviceProviderAccountBean.getServiceProviderAccount(eircAccountId.toLong());
            if (serviceProviderAccount == null) {
                throw new RuntimeException("ServiceProviderAccount by id='" + eircAccountId + "' not found");
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

        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        Form form = new Form("form");
        add(form);

        //eirc account field
        form.add(new TextField<>("accountNumber", new PropertyModel<String>(serviceProviderAccount, "accountNumber")));

        // FIO fields
        form.add(new TextField<>("lastName",   new PropertyModel<String>(serviceProviderAccount.getPerson(), "lastName")));
        form.add(new TextField<>("firstName",  new PropertyModel<String>(serviceProviderAccount.getPerson(), "firstName")));
        form.add(new TextField<>("middleName", new PropertyModel<String>(serviceProviderAccount.getPerson(), "middleName")));

        // save button
        Button save = new Button("save") {

            @Override
            public void onSubmit() {
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
                    return;
                } else if (address == null) {
                    address = new Address(addressInput.getId(), AddressEntity.BUILDING);
                }

                serviceProviderAccountBean.save(serviceProviderAccount);

                info(getString("saved"));
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

    private DomainObject findObject(Long objectId, String entity) {
        IStrategy strategy = strategyFactory.getStrategy(entity);
        return strategy.findById(objectId, true);
    }
}
