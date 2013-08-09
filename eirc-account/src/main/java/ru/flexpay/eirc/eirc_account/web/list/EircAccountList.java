package ru.flexpay.eirc.eirc_account.web.list;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.address.entity.AddressEntity;
import org.complitex.address.util.AddressRenderer;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.strategy.web.DomainObjectAccessUtil;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.dictionary.web.component.search.CollapsibleSearchPanel;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import org.complitex.template.web.component.toolbar.AddItemButton;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.strategy.EircAccountBean;
import ru.flexpay.eirc.eirc_account.web.edit.EircAccountEdit;

import javax.ejb.EJB;
import java.util.List;
import java.util.Map;

import static org.complitex.dictionary.util.PageUtil.newSorting;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircAccountList extends TemplatePage {

    @EJB
    private EircAccountBean eircAccountBean;

    @EJB
    private LocaleBean localeBean;
    private WebMarkupContainer container;
    private DataView<EircAccount> dataView;
    private CollapsibleSearchPanel searchPanel;

    private EircAccount filterObject = new EircAccount();
    private Address filterAddress;

    public EircAccountList() {
        init();
    }

    public void refreshContent(AjaxRequestTarget target) {
        container.setVisible(true);
        if (target != null) {
            dataView.setCurrentPage(0);
            target.add(container);
        }
    }

    private void init() {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);

        //Search
        final List<String> searchFilters = eircAccountBean.getSearchFilters();
        container.setVisible(true);
        add(container);

        final IModel<ShowMode> showModeModel = new Model<>(ShowMode.ACTIVE);
        searchPanel = new CollapsibleSearchPanel("searchPanel", getTemplateSession().getGlobalSearchComponentState(),
                searchFilters, new ISearchCallback() {
            @Override
            public void found(Component component, Map<String, Long> ids, AjaxRequestTarget target) {
                AddressEntity addressEntity = null;
                Long filterValue = null;
                for (int i = eircAccountBean.getSearchFilters().size() - 1; i >= 0; i--) {
                    String filterField = eircAccountBean.getSearchFilters().get(i);
                    filterValue = ids.get(filterField);
                    if (filterValue != null && filterValue > -1L) {
                        addressEntity = AddressEntity.valueOf(StringUtils.upperCase(filterField));
                        break;
                    }
                }
                if (addressEntity != null) {
                    filterAddress = new Address(filterValue, addressEntity);
                    filterObject.setAddress(filterAddress);
                } else {
                    filterObject.setAddress(null);
                }
            }
        }, ShowMode.ALL, true, showModeModel);
        add(searchPanel);
        searchPanel.initialize();

        //Form
        final Form filterForm = new Form("filterForm");
        container.add(filterForm);

        //Data Provider
        final DataProvider<EircAccount> dataProvider = new DataProvider<EircAccount>() {

            @Override
            protected Iterable<? extends EircAccount> getData(int first, int count) {
                FilterWrapper<EircAccount> filterWrapper = FilterWrapper.of(filterObject, first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setMap(ImmutableMap.<String, Object>of("address", Boolean.TRUE));

                return eircAccountBean.getEircAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<EircAccount> filterWrapper = FilterWrapper.of(new EircAccount());
                return eircAccountBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("account_number", SortOrder.ASCENDING);

        //Data View
        dataView = new DataView<EircAccount>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<EircAccount> item) {
                final EircAccount eircAccount = item.getModelObject();

                item.add(new Label("accountNumber", eircAccount.getAccountNumber()));
                item.add(new Label("person", eircAccount.getPerson() != null? eircAccount.getPerson().toString(): ""));
                item.add(new Label("address", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return eircAccount.getAddress() != null? AddressRenderer.displayAddress(
                                eircAccount.getAddress().getStreetType(), eircAccount.getAddress().getStreet(),
                                eircAccount.getAddress().getBuilding(), null, eircAccount.getAddress().getApartment(),
                                eircAccount.getAddress().getRoom(), getLocale()) : "";
                    }
                }));

                ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<WebPage>("detailsLink",
                        getEditPage(), getEditPageParams(eircAccount.getId()),
                        String.valueOf(eircAccount.getId()));
                detailsLink.add(new Label("editMessage", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return getString("edit");
                    }
                }));
                item.add(detailsLink);
            }
        };
        filterForm.add(dataView);

        //Sorting
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true, "accountNumber", "person"));

        //Reset Action
        AjaxLink reset = new AjaxLink("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                filterForm.clearInput();
                filterObject.setAddress(null);

                target.add(container);
            }
        };
        filterForm.add(reset);

        //Submit Action
        AjaxButton submit = new AjaxButton("submit", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                filterObject.setAddress(filterAddress);

                target.add(container);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
            }
        };
        filterForm.add(submit);

        //Navigator
        container.add(new PagingNavigator("navigator", dataView, getPreferencesPage(), container));
    }

    protected String getBuildingStrategyName() {
        return null;
    }

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {
        return ImmutableList.of(new AddItemButton(id) {

            @Override
            protected void onClick() {
                this.getPage().setResponsePage(getEditPage(), getEditPageParams(null));
            }

            @Override
            protected void onBeforeRender() {
                if (!DomainObjectAccessUtil.canAddNew(getBuildingStrategyName(), "building")) {
                    setVisible(false);
                }
                super.onBeforeRender();
            }
        }, new CollapsibleSearchToolbarButton(id, searchPanel));
    }

    private Class<? extends Page> getEditPage() {
        return EircAccountEdit.class;
    }

    private PageParameters getEditPageParams(Long id) {
        PageParameters parameters = new PageParameters();
        if (id != null) {
            parameters.add("objectId", id);
        }
        return parameters;
    }
}

