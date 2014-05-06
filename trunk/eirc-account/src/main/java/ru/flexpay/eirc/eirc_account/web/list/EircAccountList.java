package ru.flexpay.eirc.eirc_account.web.list;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.address.entity.AddressEntity;
import org.complitex.address.util.AddressRenderer;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.dictionary.web.component.search.CollapsibleSearchPanel;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import org.complitex.dictionary.web.component.search.IToggleCallback;
import org.complitex.template.web.component.toolbar.AddItemButton;
import org.complitex.template.web.component.toolbar.DeleteItemButton;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.eirc_account.web.edit.EircAccountEdit;

import javax.ejb.EJB;
import java.util.List;
import java.util.Map;

import static org.complitex.dictionary.util.PageUtil.newSorting;
import static org.complitex.dictionary.web.component.ShowMode.ACTIVE;
import static org.complitex.dictionary.web.component.ShowMode.ALL;

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

    private Boolean toggle = false;

    private Map<EircAccount, AjaxCheckBox> selected = Maps.newHashMap();

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

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        //Search
        final List<String> searchFilters = eircAccountBean.getSearchFilters();
        container.setVisible(true);
        add(container);


        final IModel<ShowMode> showModeModel = new Model<>(ACTIVE);
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
        }, ALL, true, showModeModel, new IToggleCallback() {
            @Override
            public void visible(boolean newState, AjaxRequestTarget target) {
                toggle = newState;
                target.add(container);
            }
        });
        add(searchPanel);
        searchPanel.initialize();

        //Form
        final Form filterForm = new Form("filterForm");
        container.add(filterForm);

        //Data Provider
        final DataProvider<EircAccount> dataProvider = new DataProvider<EircAccount>() {

            @Override
            protected Iterable<? extends EircAccount> getData(long first, long count) {
                FilterWrapper<EircAccount> filterWrapper = FilterWrapper.of(filterObject, first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setLocale(localeBean.convert(getLocale()));
                filterWrapper.getMap().put("address", Boolean.TRUE);

                setShowModel(filterWrapper);

                return eircAccountBean.getEircAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<EircAccount> filterWrapper = FilterWrapper.of(filterObject);

                setShowModel(filterWrapper);

                return eircAccountBean.count(filterWrapper);
            }

            private void setShowModel(FilterWrapper<EircAccount> filterWrapper) {
                switch (showModeModel.getObject()) {
                    case INACTIVE:
                        filterWrapper.getMap().put("inactive", Boolean.TRUE);
                        break;
                    case ALL:
                        filterWrapper.getMap().put("all", Boolean.TRUE);
                        break;
                }
            }
        };
        dataProvider.setSort("eirc_account_number", SortOrder.ASCENDING);

        final AjaxCheckBox selectAll;
        filterForm.add(selectAll = new AjaxCheckBox("selectAll", new Model<>(false)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Map.Entry<EircAccount, AjaxCheckBox> entry : selected.entrySet()) {
                    if (entry.getKey().getEndDate() == null) {
                        IModel<Boolean> model = entry.getValue().getModel();
                        model.setObject(getModelObject());
                        target.add(entry.getValue());
                    }
                }
            }
        });

        //Data View
        dataView = new DataView<EircAccount>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<EircAccount> item) {
                final EircAccount eircAccount = item.getModelObject();

                AjaxCheckBox select = new AjaxCheckBox("select", new Model<>(false)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (!getModelObject() && selectAll.getModelObject()) {
                            selectAll.setModelObject(false);
                            target.add(selectAll);
                        }
                    }
                };
                select.setVisible(eircAccount.getEndDate() == null);
                selected.put(eircAccount, select);
                item.add(select);

                item.add(new Label("accountNumber", eircAccount.getAccountNumber()));
                item.add(new Label("person", eircAccount.getPerson() != null? eircAccount.getPerson().toString(): ""));
                item.add(new Label("address", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return eircAccount.getAddress() != null?
                                (
                                        toggle?
                                                AddressRenderer.displayAddress(
                                                        eircAccount.getAddress().getCityType(), eircAccount.getAddress().getCity(),
                                                        eircAccount.getAddress().getStreetType(), eircAccount.getAddress().getStreet(),
                                                        eircAccount.getAddress().getBuilding(), null, eircAccount.getAddress().getApartment(),
                                                        eircAccount.getAddress().getRoom(), getLocale())
                                                :
                                                AddressRenderer.displayAddress(
                                                        eircAccount.getAddress().getStreetType(), eircAccount.getAddress().getStreet(),
                                                        eircAccount.getAddress().getBuilding(), null, eircAccount.getAddress().getApartment(),
                                                        eircAccount.getAddress().getRoom(), getLocale())
                                ): "";
                    }
                }));

                ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<WebPage>("detailsLink",
                        getEditPage(), getEditPageParams(eircAccount.getId()),
                        String.valueOf(eircAccount.getId()));
                detailsLink.add(new Label("editMessage", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return getString(eircAccount.getEndDate() == null? "edit" : "view");
                    }
                }));
                item.add(detailsLink);
            }
        };
        filterForm.add(dataView);

        //Sorting
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true, "eircAccountNumber", "eircAccountPerson", "eircAccountAddress"));

        //Filters
        filterForm.add(new TextField<>("accountNumberFilter", new PropertyModel<String>(filterObject, "accountNumber")));

        filterForm.add(new TextField<>("personFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterObject.getPerson() != null? filterObject.getPerson().toString() : "";
            }

            @Override
            public void setObject(String fio) {
                if (StringUtils.isBlank(fio)) {
                    filterObject.setPerson(null);
                } else {
                    fio = fio.trim();
                    String[] personFio = fio.split(" ", 3);

                    Person person = new Person();

                    if (personFio.length > 0) {
                        person.setLastName(personFio[0]);
                    }
                    if (personFio.length > 1) {
                        person.setFirstName(personFio[1]);
                    }
                    if (personFio.length > 2) {
                        person.setMiddleName(personFio[2]);
                    }

                    filterObject.setPerson(person);
                }
            }
        }));

        filterForm.add(new TextField<>("addressFilter", new Model<String>()));

        //Reset Action
        AjaxLink reset = new IndicatingAjaxLink("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                filterForm.clearInput();
                filterObject.setAddress(null);
                filterObject.setPerson(null);
                filterObject.setAccountNumber(null);

                selected.clear();

                target.add(container);
            }
        };
        filterForm.add(reset);

        //Submit Action
        AjaxButton submit = new IndicatingAjaxButton("submit", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                filterObject.setAddress(filterAddress);
                selected.clear();

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

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {
        return ImmutableList.of(
                new AddItemButton(id) {

                    @Override
                    protected void onClick() {
                        this.getPage().setResponsePage(getEditPage(), getEditPageParams(null));
                    }
                },
                new DeleteItemButton(id, true) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        for (Map.Entry<EircAccount, AjaxCheckBox> entry : selected.entrySet()) {
                            if (entry.getValue().getModelObject()) {
                                eircAccountBean.archive(entry.getKey());
                            }
                        }
                        selected.clear();
                        target.add(container);
                    }
                },
                new CollapsibleSearchToolbarButton(id, searchPanel));
    }

    private Class<? extends Page> getEditPage() {
        return EircAccountEdit.class;
    }

    private PageParameters getEditPageParams(Long id) {
        PageParameters parameters = new PageParameters();
        if (id != null) {
            parameters.add("eircAccountId", id);
        }
        return parameters;
    }
}

