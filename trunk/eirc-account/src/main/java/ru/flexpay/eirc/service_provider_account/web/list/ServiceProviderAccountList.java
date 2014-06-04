package ru.flexpay.eirc.service_provider_account.web.list;

import com.google.common.collect.ImmutableList;
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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.address.entity.AddressEntity;
import org.complitex.address.util.AddressRenderer;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.dictionary.web.component.search.CollapsibleSearchPanel;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import org.complitex.dictionary.web.component.search.IToggleCallback;
import org.complitex.template.web.component.toolbar.AddItemButton;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.search.CollapsibleSearchToolbarButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.web.component.ServicePicker;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.web.edit.ServiceProviderAccountEdit;

import javax.ejb.EJB;
import java.util.List;
import java.util.Map;

import static org.complitex.dictionary.util.PageUtil.newSorting;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceProviderAccountList extends TemplatePage {

    private static final List<String> searchFilters = ImmutableList.of("country", "region", "city", "street", "building", "apartment", "room");

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private LocaleBean localeBean;

    @EJB
    private ServiceBean serviceBean;

    private WebMarkupContainer container;
    private DataView<ServiceProviderAccount> dataView;
    private CollapsibleSearchPanel searchPanel;

    private ServiceProviderAccount filterObject = new ServiceProviderAccount(new EircAccount());
    private Address filterAddress;

    private Boolean toggle = false;

    public ServiceProviderAccountList() {
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

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);

        final Locale locale = localeBean.convert(getLocale());

        //Search
        final List<String> searchFilters = getSearchFilters();
        container.setVisible(true);
        add(container);

        final IModel<ShowMode> showModeModel = new Model<>(ShowMode.ACTIVE);
        searchPanel = new CollapsibleSearchPanel("searchPanel", getTemplateSession().getGlobalSearchComponentState(),
                searchFilters, new ISearchCallback() {
            @Override
            public void found(Component component, Map<String, Long> ids, AjaxRequestTarget target) {
                AddressEntity addressEntity = null;
                Long filterValue = null;
                for (int i = searchFilters.size() - 1; i >= 0; i--) {
                    String filterField = searchFilters.get(i);
                    filterValue = ids.get(filterField);
                    if (filterValue != null && filterValue > -1L) {
                        addressEntity = AddressEntity.valueOf(StringUtils.upperCase(filterField));
                        break;
                    }
                }
                if (addressEntity != null) {
                    filterAddress = new Address(filterValue, addressEntity);
                    filterObject.getEircAccount().setAddress(filterAddress);
                } else {
                    filterObject.getEircAccount().setAddress(null);
                }
            }
        }, ShowMode.ALL, true, showModeModel, new IToggleCallback() {
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
        final DataProvider<ServiceProviderAccount> dataProvider = new DataProvider<ServiceProviderAccount>() {

            @Override
            protected Iterable<? extends ServiceProviderAccount> getData(long first, long count) {
                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject, first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.getMap().put("address", Boolean.TRUE);
                filterWrapper.setLocale(locale);

                return serviceProviderAccountBean.getServiceProviderAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject);
                return serviceProviderAccountBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("spa_account_number", SortOrder.ASCENDING);

        //Data View
        dataView = new DataView<ServiceProviderAccount>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<ServiceProviderAccount> item) {
                final ServiceProviderAccount serviceProviderAccount = item.getModelObject();

                item.add(new Label("accountNumber", serviceProviderAccount.getAccountNumber()));
                item.add(new Label("service", serviceProviderAccount.getService().getName(locale) + " (" + serviceProviderAccount.getService().getCode() + ")" ));
                item.add(new Label("serviceProvider", serviceProviderAccount.getOrganizationName()));
                item.add(new Label("person", serviceProviderAccount.getPerson() != null? serviceProviderAccount.getPerson().toString(): ""));
                item.add(new Label("address", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return serviceProviderAccount.getEircAccount().getAddress() != null?
                                (
                                        toggle?
                                                AddressRenderer.displayAddress(
                                                        serviceProviderAccount.getEircAccount().getAddress().getCityType(), serviceProviderAccount.getEircAccount().getAddress().getCity(),
                                                        serviceProviderAccount.getEircAccount().getAddress().getStreetType(), serviceProviderAccount.getEircAccount().getAddress().getStreet(),
                                                        serviceProviderAccount.getEircAccount().getAddress().getBuilding(), null, serviceProviderAccount.getEircAccount().getAddress().getApartment(),
                                                        serviceProviderAccount.getEircAccount().getAddress().getRoom(), getLocale())
                                                :
                                                AddressRenderer.displayAddress(
                                                        serviceProviderAccount.getEircAccount().getAddress().getStreetType(), serviceProviderAccount.getEircAccount().getAddress().getStreet(),
                                                        serviceProviderAccount.getEircAccount().getAddress().getBuilding(), null, serviceProviderAccount.getEircAccount().getAddress().getApartment(),
                                                        serviceProviderAccount.getEircAccount().getAddress().getRoom(), getLocale())
                                ): "";
                    }
                }));

                ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<WebPage>("detailsLink",
                        getEditPage(), getEditPageParams(serviceProviderAccount.getId()),
                        String.valueOf(serviceProviderAccount.getId()));
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
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true, "spaAccountNumber", "serviceName", "spaOrganizationName", "spaPerson", "eircAccountAddress"));

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

        filterForm.add(new ServicePicker("serviceFilter", new PropertyModel<Service>(filterObject, "service")));

        filterForm.add(new OrganizationPicker("organizationId", filterObject, OrganizationType.SERVICE_PROVIDER.getId()));

        //Reset Action
        AjaxLink reset = new AjaxLink("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                filterForm.clearInput();
                filterObject.getEircAccount().setAddress(null);
                filterObject.setPerson(null);
                filterObject.setAccountNumber(null);
                filterObject.setService(null);
                filterObject.setOrganizationId(null);
                filterObject.setOrganizationName(null);

                target.add(container);
            }
        };
        filterForm.add(reset);

        //Submit Action
        AjaxButton submit = new AjaxButton("submit", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                filterObject.getEircAccount().setAddress(filterAddress);

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
        return ImmutableList.of(new AddItemButton(id) {

            @Override
            protected void onClick() {
                this.getPage().setResponsePage(getEditPage(), getEditPageParams(null));
            }
        }, new CollapsibleSearchToolbarButton(id, searchPanel));
    }

    private Class<? extends Page> getEditPage() {
        return ServiceProviderAccountEdit.class;
    }

    private PageParameters getEditPageParams(Long id) {
        PageParameters parameters = new PageParameters();
        if (id != null) {
            parameters.add("serviceProviderAccountId", id);
        }
        return parameters;
    }

    private List<String> getSearchFilters() {
        return searchFilters;
    }
}

