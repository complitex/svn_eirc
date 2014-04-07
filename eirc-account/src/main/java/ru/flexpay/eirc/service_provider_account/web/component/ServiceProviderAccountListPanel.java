package ru.flexpay.eirc.service_provider_account.web.component;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.web.edit.ServiceProviderAccountEdit;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class ServiceProviderAccountListPanel extends Panel {

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private LocaleBean localeBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    private WebMarkupContainer container;

    private SingleSortState<String> state = new SingleSortState<>();
    private String sortProperty = "spa_account_number";

    private ServiceProviderAccount filterObject = new ServiceProviderAccount(new EircAccount());

    public ServiceProviderAccountListPanel(String id, Long eircAccountId) {
        super(id);
        filterObject.getEircAccount().setId(eircAccountId);
        init();
    }

    private void init() {

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);

        add(container);

        final Locale locale = localeBean.convert(getLocale());

        final List<IColumn<ServiceProviderAccount, String>> COLUMNS = ImmutableList.<IColumn<ServiceProviderAccount, String>>of(
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("accountNumber"), "spa_account_number") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new TextFilter<>(s, new PropertyModel<String>(filterObject, "accountNumber"), components);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        components.add(new Label(s, serviceProviderAccountIModel.getObject().getAccountNumber()));
                    }
                },
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("service"), "service_name") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new ChoiceFilter<>(s,
                                new PropertyModel<Service>(filterObject, "service"),
                                components,
                                serviceBean.getServices(FilterWrapper.of(new Service())),
                                new IChoiceRenderer<Service>() {
                                    @Override
                                    public Object getDisplayValue(Service service) {
                                        return service.getName(locale) + " (" + service.getCode() + ")";
                                    }

                                    @Override
                                    public String getIdValue(Service service, int i) {
                                        return service.getId().toString();
                                    }
                                },
                                false
                        );
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        components.add(new Label(s, serviceProviderAccount.getService().getName(locale) + " (" + serviceProviderAccount.getService().getCode() + ")"));
                    }
                },
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("serviceProvider"), "spa_organization_name") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new ChoiceFilter<>(s,
                                new IModel<DomainObject>() {

                                    @Override
                                    public DomainObject getObject() {
                                        return filterObject.getOrganizationId() != null ?
                                                organizationStrategy.findById(filterObject.getOrganizationId(), false) :
                                                null;
                                    }

                                    @Override
                                    public void setObject(DomainObject domainObject) {
                                        if (domainObject != null) {
                                            filterObject.setOrganizationId(domainObject.getId());
                                        } else {
                                            filterObject.setOrganizationId(null);
                                        }
                                    }

                                    @Override
                                    public void detach() {

                                    }
                                },
                                components,
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
                                },
                                false
                        );
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        components.add(new Label(s, serviceProviderAccount.getOrganizationName()));
                    }
                },
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("person"), "spa_person") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new TextFilter<>(s, new Model<String>() {

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
                        }, components);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        components.add(new Label(s, serviceProviderAccount.getPerson() != null? serviceProviderAccount.getPerson().toString(): ""));
                    }
                },
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("empty")) {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new AjaxGoAndClearFilter(s, components, new ResourceModel("filter"), new ResourceModel("clear")) {
                            @Override
                            public void onGoSubmit(AjaxRequestTarget target, Form<?> form) {
                                target.add(container);
                            }

                            @Override
                            public void onClearSubmit(AjaxRequestTarget target, Form<?> form) {
                                filterObject = new ServiceProviderAccount(filterObject.getEircAccount());
                                target.add(container);
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<>(s,
                                getEditPage(), getEditPageParams(serviceProviderAccount.getId()),
                                String.valueOf(serviceProviderAccount.getId()),
                                new ResourceModel("edit"));
                        components.add(detailsLink);
                    }
                }
        );

        DataProvider<ServiceProviderAccount> provider = new DataProvider<ServiceProviderAccount>() {

            @Override
            protected Iterable<? extends ServiceProviderAccount> getData(long first, long count) {
                final SortOrder order = state.getPropertySortOrder(sortProperty);

                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject, first, count);
                filterWrapper.setSortProperty(sortProperty);
                filterWrapper.setAscending(SortOrder.ASCENDING.equals(order));
                filterWrapper.getMap().put("address", Boolean.FALSE);
                filterWrapper.setLocale(locale);

                return serviceProviderAccountBean.getServiceProviderAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject);
                return serviceProviderAccountBean.count(filterWrapper);
            }
        };

        final IFilterStateLocator<ServiceProviderAccount> locator = new IFilterStateLocator<ServiceProviderAccount>() {
            @Override
            public ServiceProviderAccount getFilterState() {
                return filterObject;
            }

            @Override
            public void setFilterState(ServiceProviderAccount state) {
                filterObject = state;
            }
        };

        FilterForm<ServiceProviderAccount> filterForm = new FilterForm<>("filterForm", locator);

        final DataTable<ServiceProviderAccount, String> table = new DataTable<ServiceProviderAccount, String>("datatable", COLUMNS, provider, 1000) {
            private AttributeAppender style = new AttributeAppender("class", new Model<>("selected"));

            @Override
            protected Item<ServiceProviderAccount> newRowItem(String id, int index, final IModel<ServiceProviderAccount> model) {
                return super.newRowItem(id, index, model);
            }
        };
        table.addTopToolbar(new HeadersToolbar<String>(table, new ISortStateLocator<String>() {
            @Override
            public ISortState<String> getSortState() {
                return state;
            }
        }) {
            @Override
            protected WebMarkupContainer newSortableHeader(final String headerId, final String property,
                                                           final ISortStateLocator<String> locator)
            {
                return new AjaxFallbackOrderByBorder<String>(headerId, property, locator) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onAjaxClick(AjaxRequestTarget target) {
                        sortProperty = property;
                        target.add(container);
                    }
                };
            }
        });
        table.addTopToolbar(new FilterToolbar(table, filterForm, locator));
        container.add(filterForm);
        filterForm.add(table);
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
}
