package ru.flexpay.eirc.eirc_account.web.edit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
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

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircAccountEdit extends FormTemplatePage {

    @EJB
    private StrategyFactory strategyFactory;

    private WebMarkupContainer container;
    private SearchComponentState componentState;

    private Long cityId;
    private Long streetTypeId;
    private Long streetId;
    private Long buildingId;


    private static final Logger log = LoggerFactory.getLogger(EircAccountEdit.class);

    private void init() {

        IModel<String> labelModel = new StringResourceModel("label", null, null);
        Label title = new Label("title", labelModel);
        add(title);
        final Label label = new Label("label", labelModel);
        label.setOutputMarkupId(true);
        add(label);

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        //Form<Void> form = new Form<Void>("form");

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(false);

        //address
        componentState = new SearchComponentState();
        WiQuerySearchComponent searchComponent =
                new WiQuerySearchComponent("searchComponent", componentState, Lists.<String>newArrayList(), null, ShowMode.ACTIVE, true);
        container.add(searchComponent);

        AjaxLink<Void> save = new AjaxLink<Void>("save") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        container.add(save);

        AjaxLink<Void> cancel = new AjaxLink<Void>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        container.add(cancel);
    }

    private void initSearchComponentState(SearchComponentState componentState) {
        componentState.clear();

        if (cityId != null) {
            componentState.put("city", findObject(cityId, "city"));
        }

        if (streetId != null) {
            componentState.put("street", findObject(streetId, "street"));
        }

        if (buildingId != null) {
            componentState.put("building", findObject(buildingId, "building"));
        }
    }

    private DomainObject findObject(Long objectId, String entity) {
        IStrategy strategy = strategyFactory.getStrategy(entity);
        return strategy.findById(objectId, true);
    }
}
