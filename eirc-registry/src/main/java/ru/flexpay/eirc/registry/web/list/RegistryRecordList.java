package ru.flexpay.eirc.registry.web.list;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.address.entity.AddressEntity;
import org.complitex.correction.service.AddressService;
import org.complitex.correction.service.exception.DuplicateCorrectionException;
import org.complitex.correction.service.exception.MoreOneCorrectionException;
import org.complitex.correction.service.exception.NotFoundCorrectionException;
import org.complitex.correction.web.component.AddressCorrectionPanel;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.description.ILocalizedType;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.ajax.AjaxLinkPanel;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.AjaxNavigationToolbar;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.dictionary.entity.EircConfig;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.RegistryBean;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.RegistryRecordBean;
import ru.flexpay.eirc.registry.service.handle.AbstractMessenger;
import ru.flexpay.eirc.registry.service.link.RegistryLinker;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;
import ru.flexpay.eirc.registry.web.component.IMessengerContainer;
import ru.flexpay.eirc.registry.web.component.StatusDetailPanel;
import ru.flexpay.eirc.service_provider_account.web.component.AjaxGoAndClearFilter;

import javax.ejb.EJB;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class RegistryRecordList extends TemplatePage {

    private static final SimpleDateFormat OPERATION_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @EJB
    private RegistryRecordBean registryRecordBean;

    @EJB
    private RegistryBean registryBean;

    @EJB
    private RegistryWorkflowManager registryWorkflowManager;

    @EJB
    private AddressService addressService;

    @EJB
    private RegistryLinker registryLinker;

    @EJB
    private RegistryMessenger imessengerService;

    private AbstractMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallbackService;

    private AbstractFinishCallback finishCallback;

    @EJB
    private ConfigBean configBean;

    @EJB
    private ModuleInstanceStrategy moduleInstanceStrategy;

    private CompoundPropertyModel<RegistryRecordData> filterModel = new CompoundPropertyModel<RegistryRecordData>(new RegistryRecord());

    private Registry registry;

    private IMessengerContainer container;

    public RegistryRecordList(PageParameters params) throws ExecutionException, InterruptedException {
        imessenger = imessengerService.getInstance();
        finishCallback = finishCallbackService.getInstance();

        StringValue registryIdParam = params.get("registryId");
        if (registryIdParam == null || registryIdParam.isEmpty()) {
            getSession().error(getString("error_registryId_not_found"));
            setResponsePage(RegistryList.class);
            return;
        }
        List<Registry> registries = registryBean.getRegistries(FilterWrapper.of(new Registry(registryIdParam.toLong())));
        if (registries.size() == 0) {
            getSession().error(getString("error_registry_not_found"));
            setResponsePage(RegistryList.class);
            return;
        }
        registry = registries.get(0);
        ((RegistryRecord)filterModel.getObject()).setRegistryId(registry.getId());
        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        container = new IMessengerContainer("container", imessenger, finishCallback) {
            @Override
            protected boolean isCompleted() {
                return !registryWorkflowManager.isInWork(registry) || super.isCompleted();
            }
        };
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        container.add(messages);

        final IFilterStateLocator<RegistryRecordData> locator = new IFilterStateLocator<RegistryRecordData>() {
            @Override
            public RegistryRecordData getFilterState() {
                return filterModel.getObject();
            }

            @Override
            public void setFilterState(RegistryRecordData state) {
                filterModel.setObject(state);
            }
        };

        //Form
        final FilterForm<RegistryRecordData> filterForm = new FilterForm<>("filterForm", locator);
        filterForm.setOutputMarkupId(true);
        container.add(filterForm);

        AjaxLazyLoadPanel statusDetailPanel = new AjaxLazyLoadPanel("statusDetailPanel") {
            @Override
            public Component getLazyLoadComponent(String markupId) {
                return new StatusDetailPanel(markupId, filterModel, filterForm);
            }
        };
        statusDetailPanel.setOutputMarkupId(true);
        container.add(statusDetailPanel);

        Long moduleId = null;
        try {
            moduleId = configBean.getInteger(EircConfig.MODULE_ID, true).longValue();
        } catch (Exception e) {
            log().error("Can not get {} from config: {}", EircConfig.MODULE_ID, e.toString());
        }

        DomainObject module = moduleInstanceStrategy.findById(moduleId, true);

        final Integer userOrganizationId = AttributeUtil.getIntegerValue(module, ModuleInstanceStrategy.ORGANIZATION);

        //Панель коррекции адреса
        final AddressCorrectionPanel<RegistryRecordData> addressCorrectionPanel = new AddressCorrectionPanel<RegistryRecordData>("addressCorrectionPanel",
                userOrganizationId != null? userOrganizationId.longValue() : -1 , container) {

            @Override
            protected void correctAddress(RegistryRecordData registryRecord, AddressEntity entity, Long cityId, Long streetTypeId, Long streetId,
                                          Long buildingId, Long apartmentId, Long roomId, Long userOrganizationId)
                    throws DuplicateCorrectionException, MoreOneCorrectionException, NotFoundCorrectionException {

                if (registryWorkflowManager.canLink(registry)) {

                    if (userOrganizationId == null) {
                        RegistryRecordList.this.container.error("failed_user_organization");
                        return;
                    }

                    initTimerBehavior();

                    addressService.correctAddress(registryRecord, entity, cityId, streetTypeId, streetId, buildingId, apartmentId, roomId,
                            userOrganizationId, registry.getSenderOrganizationId());

                    registryLinker.linkAfterCorrection(registryRecord, imessenger, finishCallback);

                } else {
                    RegistryRecordList.this.container.error("failed_correction_registry_linking");
                }
            }

            @Override
            protected void closeDialog(AjaxRequestTarget target) {
                showIMessages(target);
                super.closeDialog(target);
            }
        };
        add(addressCorrectionPanel);


        ImmutableList.Builder<IColumn<RegistryRecordData, String>> builder = ImmutableList.builder();
        builder.add(buildTextColumn("registry_record_service_code", "serviceCode"));
        builder.add(buildTextColumn("registry_record_personal_account_ext", "personalAccountExt"));
        builder.add(buildTextColumn("registry_record_city_type", "cityType"));
        builder.add(buildTextColumn("registry_record_city", "city"));
        builder.add(buildTextColumn("registry_record_street_type", "streetType"));
        builder.add(buildTextColumn("registry_record_street", "street"));
        builder.add(buildTextColumn("registry_record_building_number", "buildingNumber"));
        builder.add(buildTextColumn("registry_record_building_corp", "buildingCorp"));
        builder.add(buildTextColumn("registry_record_apartment", "apartment"));
        builder.add(buildTextColumn("registry_record_room", "room"));
        builder.add(buildFioColumn("registry_record_fio"));
        builder.add(buildChoicesColumn("registry_record_import_error_type", "importErrorType", Arrays.asList(ImportErrorType.values())));
        builder.add(buildChoicesColumn("registry_record_status", "status", Arrays.asList(RegistryRecordStatus.values())));

        builder.add(
                new FilteredAbstractColumn<RegistryRecordData, String>(new ResourceModel("empty"), "action") {
                    @Override
                    public Component getFilter(String s, FilterForm<?> components) {
                        return new AjaxGoAndClearFilter(s, components, new ResourceModel("find"), new ResourceModel("reset")) {
                            @Override
                            public void onGoSubmit(AjaxRequestTarget target, Form<?> form) {
                                target.add(container);
                            }

                            @Override
                            public void onClearSubmit(AjaxRequestTarget target, Form<?> form) {
                                filterForm.clearInput();
                                Long registryId = filterModel.getObject().getRegistryId();

                                filterModel.setObject(new RegistryRecord(registryId));

                                target.add(container);
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<RegistryRecordData>> components, String s,
                                             IModel<RegistryRecordData> serviceProviderAccountIModel) {

                        final RegistryRecordData registryRecord = serviceProviderAccountIModel.getObject();
                        AjaxLinkPanel addressCorrectionLink = new AjaxLinkPanel(s, new ResourceModel("correctAddress")) {

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                addressCorrectionPanel.open(target, registryRecord, registryRecord.getFirstName(),
                                        registryRecord.getMiddleName(), registryRecord.getLastName(),
                                        registryRecord.getCity(), registryRecord.getStreetType(), registryRecord.getStreet(),
                                        registryRecord.getBuildingNumber(), registryRecord.getBuildingCorp(),
                                        registryRecord.getApartment(), registryRecord.getRoom(),
                                        registryRecord.getCityId(), registryRecord.getStreetTypeId(), registryRecord.getStreetId(),
                                        registryRecord.getBuildingId(),
                                        registryRecord.getApartmentId(), registryRecord.getRoomId());
                            }
                        };
                        addressCorrectionLink.setVisible(registryRecord.getStatus() == RegistryRecordStatus.LINKED_WITH_ERROR &&
                                        registryRecord.getImportErrorType() != null &&
                                        (registryRecord.getImportErrorType().getId() < 17 || registryRecord.getImportErrorType().getId() > 18) &&
                                        registryWorkflowManager.canLink(registry)
                        );
                        components.add(addressCorrectionLink);
                    }
                });


        final List<IColumn<RegistryRecordData, String>> COLUMNS = builder.build();

        //Data Provider
        final DataProvider<RegistryRecordData> provider = new DataProvider<RegistryRecordData>() {

            @Override
            protected Iterable<? extends RegistryRecordData> getData(long first, long count) {
                FilterWrapper<RegistryRecordData> filterWrapper = FilterWrapper.of(filterModel.getObject(), first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());

                return registryRecordBean.getRegistryRecords(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<RegistryRecordData> filterWrapper = FilterWrapper.of(filterModel.getObject());
                return registryRecordBean.count(filterWrapper);
            }
        };
        provider.setSort("registry_record_id", SortOrder.ASCENDING);

        final DataTable<RegistryRecordData, String> table = new DataTable<>("datatable", COLUMNS, provider, 10);
        table.setOutputMarkupId(true);
        table.setVersioned(false);
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<>(table, provider));
        table.addTopToolbar(new FilterToolbar(table, filterForm, locator));
        table.addBottomToolbar(new AjaxNavigationToolbar(table));
        container.add(filterForm);
        filterForm.add(table);

        add(new Link("back") {
            @Override
            public void onClick() {
                setResponsePage(RegistryList.class);
            }
        });
    }

    private FilteredAbstractColumn<RegistryRecordData, String> buildTextColumn(final String sortColumn, final String propertyName) {
        return new FilteredAbstractColumn<RegistryRecordData, String>(new ResourceModel(sortColumn), sortColumn) {
            @Override
            public Component getFilter(String s, FilterForm<?> components) {
                return new TextFilter<>(s, filterModel.bind(propertyName), components);
            }

            @Override
            public void populateItem(Item<ICellPopulator<RegistryRecordData>> components, String s,
                                     IModel<RegistryRecordData> registryRecordIModel) {
                components.add(new Label(s, new PropertyModel<RegistryRecordData>(registryRecordIModel.getObject(), propertyName)));
            }
        };
    }

    private <T extends ILocalizedType> FilteredAbstractColumn<RegistryRecordData, String> buildChoicesColumn(final String sortColumn, final String propertyName, final List<T> choices) {
        return new FilteredAbstractColumn<RegistryRecordData, String>(new ResourceModel(sortColumn), sortColumn) {
            @Override
            public Component getFilter(String s, FilterForm<?> components) {
                return new ChoiceFilter<T>(s, filterModel.<T>bind(propertyName), components, choices, new ChoiceRenderer<T>() {
                    @Override
                    public Object getDisplayValue(T object) {
                        return object.getLabel(getLocale());
                    }
                }, false);
            }

            @Override
            public void populateItem(Item<ICellPopulator<RegistryRecordData>> components, String s,
                                     IModel<RegistryRecordData> registryRecordIModel) {
                components.add(new Label(s, new CompoundPropertyModel<>(registryRecordIModel.getObject()).<T>bind(propertyName).getObject().getLabel(getLocale())));
            }
        };
    }

    private FilteredAbstractColumn<RegistryRecordData, String> buildFioColumn(final String sortColumn) {
        return new FilteredAbstractColumn<RegistryRecordData, String>(new ResourceModel(sortColumn), sortColumn) {
            @Override
            public Component getFilter(String s, FilterForm<?> components) {
                return new TextFilter<>(s, new Model<String>() {

                    @Override
                    public String getObject() {
                        Person person = filterModel.getObject().getPerson();
                        return person != null ? person.toString() : "";
                    }

                    @Override
                    public void setObject(String fio) {
                        RegistryRecord registryRecord = (RegistryRecord) filterModel.getObject();
                        if (StringUtils.isBlank(fio)) {
                            registryRecord.setLastName(null);
                            registryRecord.setFirstName(null);
                            registryRecord.setMiddleName(null);
                        } else {
                            fio = fio.trim();
                            String[] personFio = fio.split(" ", 3);

                            if (personFio.length > 0) {
                                registryRecord.setLastName(personFio[0]);
                            }
                            if (personFio.length > 1) {
                                registryRecord.setFirstName(personFio[1]);
                            } else {
                                registryRecord.setFirstName(null);
                            }
                            if (personFio.length > 2) {
                                registryRecord.setMiddleName(personFio[2]);
                            } else {
                                registryRecord.setMiddleName(null);
                            }

                        }
                    }
                }, components);
            }

            @Override
            public void populateItem(Item<ICellPopulator<RegistryRecordData>> components, String s,
                                     IModel<RegistryRecordData> registryRecordIModel) {
                Person person = registryRecordIModel.getObject().getPerson();
                components.add(new Label(s, person != null? person.toString() : ""));
            }
        };
    }

    private void initTimerBehavior() {
        container.initTimerBehavior();
    }

    private void showIMessages(AjaxRequestTarget target) {
        container.showIMessages(target);
    }
}
