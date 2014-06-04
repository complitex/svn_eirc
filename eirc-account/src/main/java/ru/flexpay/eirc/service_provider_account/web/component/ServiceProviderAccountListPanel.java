package ru.flexpay.eirc.service_provider_account.web.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.ShowModePanel;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
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

import static org.complitex.dictionary.web.component.ShowMode.ACTIVE;
import static org.complitex.dictionary.web.component.ShowMode.ALL;

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

    private WebMarkupContainer container;

    private SingleSortState<String> state = new SingleSortState<>();
    private String sortProperty = "spa_account_number";

    private ServiceProviderAccount filterObject = new ServiceProviderAccount(new EircAccount());

    private Map<ServiceProviderAccount, Model<Boolean>> selected = Maps.newHashMap();
    private List<AjaxCheckBox> checkBoxes = Lists.newArrayList();

    private boolean editable = true;

    public ServiceProviderAccountListPanel(String id, Long eircAccountId, boolean editable) {
        super(id);
        filterObject.getEircAccount().setId(eircAccountId);
        this.editable = editable;
        init();
    }

    private void init() {

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);

        add(container);

        final Model<ShowMode> showModeModel = new Model<>(editable ? ACTIVE : ALL);
        container.add(new ShowModePanel("showModelPanel", showModeModel));

        final Locale locale = localeBean.convert(getLocale());

        ImmutableList.Builder<IColumn<ServiceProviderAccount, String>> builder = ImmutableList.builder();
        if (editable) {
            builder.add(
                    new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("empty")) {
                        CheckBoxSelectorFilter filter = null;

                        @Override
                        public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> cellItem, String componentId,
                                                 IModel<ServiceProviderAccount> rowModel) {
                            if (rowModel.getObject().getEndDate() == null) {
                                final Model<Boolean> model = new Model<>();
                                selected.put(rowModel.getObject(), model);
                                if (filter != null) {
                                    filter.setModelObject(false);
                                }
                                AjaxCheckBoxPanel ajaxCheckBoxPanel = new AjaxCheckBoxPanel(componentId, model) {
                                    @Override
                                    public void onUpdate(AjaxRequestTarget target) {
                                        if (!model.getObject() && filter.getModelObject()) {
                                            filter.setModelObject(false);
                                            filter.onUpdate(target);
                                        }
                                    }
                                };
                                checkBoxes.add(ajaxCheckBoxPanel.getCheckBox());
                                cellItem.add(ajaxCheckBoxPanel);
                            } else {
                                cellItem.add(new Label(componentId, new ResourceModel("empty")));
                            }
                        }

                        @Override
                        public Component getFilter(String componentId, FilterForm<?> form) {
                            //return new CheckBox(componentId, new Model<>(false));
                            filter = new CheckBoxSelectorFilter(componentId, form, checkBoxes);
                            return filter;
                        }
                    });
        }
        builder.add(
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("spa_accountNumber"), "spa_account_number") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new TextFilter<>(s, new PropertyModel<String>(filterObject, "accountNumber"), components);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        components.add(new Label(s, serviceProviderAccountIModel.getObject().getAccountNumber()));
                    }
                });
        builder.add(
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("service"), "service_name") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new AbstractFilter<Service>(s, components, new PropertyModel<Service>(filterObject, "service")) {
                            @Override
                            protected Component createFilterComponent(String id, IModel<Service> model) {
                                return new ServicePicker(id, model);
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        components.add(new Label(s, serviceProviderAccount.getService().getName(locale) + " (" + serviceProviderAccount.getService().getCode() + ")"));
                    }
                });
        builder.add(
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("serviceProvider"), "spa_organization_name") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new AbstractFilter<Long>(s, components, new PropertyModel<Long>(filterObject, "organizationId")) {
                            @Override
                            protected Component createFilterComponent(String id, IModel<Long> model) {
                                return new OrganizationPicker(id, model, OrganizationType.SERVICE_PROVIDER.getId());
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<ServiceProviderAccount>> components, String s,
                                             IModel<ServiceProviderAccount> serviceProviderAccountIModel) {
                        ServiceProviderAccount serviceProviderAccount = serviceProviderAccountIModel.getObject();
                        components.add(new Label(s, serviceProviderAccount.getOrganizationName()));
                    }
                });
        builder.add(
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
                });
        builder.add(
                new FilteredAbstractColumn<ServiceProviderAccount, String>(new ResourceModel("empty"), "action") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new AjaxGoAndClearFilter(s, components, new ResourceModel("filter"), new ResourceModel("clear")) {
                            @Override
                            public void onGoSubmit(AjaxRequestTarget target, Form<?> form) {
                                target.add(container);
                            }

                            @Override
                            public void onClearSubmit(AjaxRequestTarget target, Form<?> form) {
                                filterObject.setAccountNumber(null);
                                filterObject.setOrganizationId(null);
                                filterObject.setService(null);
                                filterObject.setPerson(null);
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
                                serviceProviderAccount.getEndDate() == null? new ResourceModel("edit") : new ResourceModel("view"));
                        components.add(detailsLink);
                    }
                });


        final List<IColumn<ServiceProviderAccount, String>> COLUMNS = builder.build();

        DataProvider<ServiceProviderAccount> provider = new DataProvider<ServiceProviderAccount>() {

            @Override
            protected Iterable<? extends ServiceProviderAccount> getData(long first, long count) {
                final SortOrder order = state.getPropertySortOrder(sortProperty);

                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject, first, count);
                filterWrapper.setSortProperty(sortProperty);
                filterWrapper.setAscending(SortOrder.ASCENDING.equals(order));
                filterWrapper.getMap().put("address", Boolean.FALSE);
                filterWrapper.setLocale(locale);

                setShowModel(filterWrapper);

                selected.clear();
                checkBoxes.clear();

                return serviceProviderAccountBean.getServiceProviderAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<ServiceProviderAccount> filterWrapper = FilterWrapper.of(filterObject);

                setShowModel(filterWrapper);

                return serviceProviderAccountBean.count(filterWrapper);
            }

            private void setShowModel(FilterWrapper<ServiceProviderAccount> filterWrapper) {
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

        final FilterForm<ServiceProviderAccount> filterForm = new FilterForm<>("filterForm", locator);

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
                                                           final ISortStateLocator<String> locator) {
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

        filterForm.add(new AjaxButton("add") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                this.getPage().setResponsePage(getEditPage(), getEditPageParams(null));
            }
        }.setVisible(editable));

        filterForm.add(new AjaxButton("delete") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                for (Map.Entry<ServiceProviderAccount, Model<Boolean>> entry : selected.entrySet()) {
                    if (entry.getValue().getObject()) {
                        serviceProviderAccountBean.archive(entry.getKey());
                    }
                }
                target.add(container);
            }
        }.setVisible(editable));
    }

    private Class<? extends Page> getEditPage() {
        return ServiceProviderAccountEdit.class;
    }

    private PageParameters getEditPageParams(Long serviceProviderAccountId) {
        PageParameters parameters = new PageParameters();
        if (serviceProviderAccountId != null) {
            parameters.add("serviceProviderAccountId", serviceProviderAccountId);
        }
        parameters.add("eircAccountId", filterObject.getEircAccount().getId());
        parameters.add("revertToEircAccount", Boolean.TRUE);
        return parameters;
    }
}
