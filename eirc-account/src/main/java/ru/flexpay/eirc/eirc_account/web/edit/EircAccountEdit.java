package ru.flexpay.eirc.eirc_account.web.edit;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.strategy.StrategyFactory;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import org.complitex.dictionary.web.component.search.WiQuerySearchComponent;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.strategy.EircAccountBean;

import javax.ejb.EJB;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircAccountEdit extends FormTemplatePage {

    @EJB
    private StrategyFactory strategyFactory;

    @EJB
    private EircAccountBean eircAccountBean;

    private SearchComponentState componentState;

    private EircAccount eircAccount;


    private static final Logger log = LoggerFactory.getLogger(EircAccountEdit.class);

    public EircAccountEdit() {
        init();
    }

    public EircAccountEdit(PageParameters parameters) {
        StringValue eircAccountId = parameters.get("eircAccountId");
        if (eircAccountId != null) {
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

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        Form form = new Form("form");
        add(form);

        //address component
        componentState = new SearchComponentState();
        WiQuerySearchComponent searchComponent =
                new WiQuerySearchComponent("searchComponent", componentState, eircAccountBean.getSearchFilters(), null, ShowMode.ACTIVE, true);
        form.add(searchComponent);

        //eirc account field
        form.add(new TextField<>("accountNumber", new PropertyModel<String>(eircAccount, "accountNumber")));

        // FIO fields
        form.add(new TextField<>("lastName",   new PropertyModel<String>(eircAccount.getPerson(), "lastName")));
        form.add(new TextField<>("firstName",  new PropertyModel<String>(eircAccount.getPerson(), "firstName")));
        form.add(new TextField<>("middleName", new PropertyModel<String>(eircAccount.getPerson(), "middleName")));

        // save button
        AjaxLink<Void> save = new AjaxLink<Void>("save") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        form.add(save);

        // cancel button
        AjaxLink<Void> cancel = new AjaxLink<Void>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        form.add(cancel);
    }

    private void initSearchComponentState(SearchComponentState componentState) {
        componentState.clear();
        /*
        if (cityId != null) {
            componentState.put("city", findObject(cityId, "city"));
        }

        if (streetId != null) {
            componentState.put("street", findObject(streetId, "street"));
        }

        if (buildingId != null) {
            componentState.put("building", findObject(buildingId, "building"));
        }*/
    }

    private DomainObject findObject(Long objectId, String entity) {
        IStrategy strategy = strategyFactory.getStrategy(entity);
        return strategy.findById(objectId, true);
    }
}
