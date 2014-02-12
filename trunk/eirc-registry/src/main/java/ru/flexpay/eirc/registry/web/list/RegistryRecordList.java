package ru.flexpay.eirc.registry.web.list;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;
import org.complitex.address.entity.AddressEntity;
import org.complitex.correction.service.AddressService;
import org.complitex.correction.service.exception.DuplicateCorrectionException;
import org.complitex.correction.service.exception.MoreOneCorrectionException;
import org.complitex.correction.service.exception.NotFoundCorrectionException;
import org.complitex.correction.web.component.AddressCorrectionPanel;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.web.component.DatePicker;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.template.web.template.FormTemplatePage;
import org.complitex.template.web.template.TemplatePage;
import org.odlabs.wiquery.ui.accordion.Accordion;
import org.odlabs.wiquery.ui.accordion.AccordionActive;
import org.odlabs.wiquery.ui.accordion.AccordionAnimated;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.RegistryBean;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.RegistryRecordBean;
import ru.flexpay.eirc.registry.service.link.RegistryLinker;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;

import javax.ejb.EJB;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.complitex.dictionary.util.PageUtil.newSorting;

/**
 * @author Pavel Sknar
 */
public class RegistryRecordList extends TemplatePage {

    private static final SimpleDateFormat OPERATION_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

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
    private RegistryMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallback;

    private IModel<RegistryRecordData> filterModel = new CompoundPropertyModel<RegistryRecordData>(new RegistryRecord());

    private Registry registry;

    private WebMarkupContainer container;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    public RegistryRecordList(PageParameters params) throws ExecutionException, InterruptedException {
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

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        container.add(messages);

        if (imessenger.countIMessages() > 0 || !finishCallback.isCompleted()) {
            timerBehavior = new MessageBehavior(Duration.seconds(5));
            container.add(timerBehavior);
        }

        //Form
        final Form<RegistryRecordData> filterForm = new Form<>("filterForm", filterModel);
        container.add(filterForm);

        //Панель коррекции адреса
        final AddressCorrectionPanel<RegistryRecordData> addressCorrectionPanel = new AddressCorrectionPanel<RegistryRecordData>("addressCorrectionPanel",
                registry.getRecipientOrganizationId(), container) {

            @Override
            protected void correctAddress(RegistryRecordData registryRecord, AddressEntity entity, Long cityId, Long streetTypeId, Long streetId,
                                          Long buildingId, Long apartmentId, Long roomId, Long userOrganizationId)
                    throws DuplicateCorrectionException, MoreOneCorrectionException, NotFoundCorrectionException {

                if (registryWorkflowManager.canLink(registry)) {

                    if (timerBehavior == null) {
                        timerBehavior = new MessageBehavior(Duration.seconds(5));
                        container.add(timerBehavior);
                    }

                    addressService.correctAddress(registryRecord, entity, cityId, streetTypeId, streetId, buildingId, apartmentId, roomId,
                            registry.getRecipientOrganizationId(), registry.getSenderOrganizationId());

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

        //Data Provider
        final DataProvider<RegistryRecordData> dataProvider = new DataProvider<RegistryRecordData>() {

            @Override
            protected Iterable<? extends RegistryRecordData> getData(int first, int count) {
                FilterWrapper<RegistryRecordData> filterWrapper = FilterWrapper.of(filterModel.getObject(), first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setLike(true);

                return registryRecordBean.getRegistryRecords(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<RegistryRecordData> filterWrapper = FilterWrapper.of(filterModel.getObject());
                return registryRecordBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("registry_record_id", SortOrder.ASCENDING);

        //Data View
        DataView<RegistryRecordData> dataView = new DataView<RegistryRecordData>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<RegistryRecordData> item) {
                final RegistryRecordData registryRecord = item.getModelObject();

                item.setModel(new CompoundPropertyModel<>(item.getModel()));

                item.add(new Label("serviceCode"));
                item.add(new Label("personalAccountExt"));
                item.add(new Label("cityType"));
                item.add(new Label("city"));
                item.add(new Label("streetType"));
                item.add(new Label("street"));
                item.add(new Label("buildingNumber"));
                item.add(new Label("buildingCorp"));
                item.add(new Label("apartment"));
                item.add(new Label("room"));
                item.add(new Label("lastName"));
                item.add(new Label("firstName"));
                item.add(new Label("middleName"));
                item.add(new Label("operationDate", registryRecord.getOperationDate() != null ?
                        OPERATION_DATE_FORMAT.format(registryRecord.getOperationDate()) : ""));
                item.add(new Label("amount"));

                Accordion accordion = new Accordion("accordionContainers");
                accordion.setCollapsible(true);
                accordion.setClearStyle(true);
                accordion.setNavigation(true);
                accordion.setActive(new AccordionActive(false));
                accordion.setAnimated(new AccordionAnimated(false));
                accordion.setOutputMarkupPlaceholderTag(true);
                accordion.setAutoHeight(false);
                item.add(accordion);

                final DataProvider<Container> dataProvider = new DataProvider<Container>() {
                    @Override
                    protected Iterable<? extends Container> getData(int first, int count) {
                        return registryRecord.getContainers();
                    }

                    @Override
                    protected int getSize() {
                        return registryRecord.getContainers().size();
                    }
                };

                DataView<Container> dataView = new DataView<Container>("containersData", dataProvider,
                        registryRecord.getContainers().size()) {
                    @Override
                    protected void populateItem(Item<Container> item) {
                        item.add(new Label("container", item.getModelObject().getData()));
                    }
                };

                accordion.add(dataView);

                item.add(new Label("importErrorType", registryRecord.getImportErrorType() != null?
                        registryRecord.getImportErrorType().getLabel(getLocale()) : ""));
                item.add(new Label("status", registryRecord.getStatus().getLabel(getLocale())));

                /*
                ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<>("detailsLink",
                        getEditPage(), getEditPageParams(registryRecord.getId()),
                        String.valueOf(registryRecord.getId()));
                detailsLink.add(new Label("editMessage", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return getString("edit");
                    }
                }));
                detailsLink.setEnabled(false);
                item.add(detailsLink);
                */

                AjaxLink addressCorrectionLink = new IndicatingAjaxLink("addressCorrectionLink") {

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
                        (registryRecord.getImportErrorType().getId() < 17 || registryRecord.getImportErrorType().getId() > 18));
                item.add(addressCorrectionLink);
            }
        };
        filterForm.add(dataView);

        //Sorting
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true,
                "registryRecordServiceCode", "registryRecordPersonalAccountExt",
                "registryRecordCityType",    "registryRecordCity",
                "registryRecordStreetType", "registryRecordStreet",
                "registryRecordBuildingNumber", "registryRecordBulkNumber",
                "registryRecordApartmentNumber", "registryRecordRoomNumber",
                "registryRecordFio",
                "registryRecordOperationDate", "registryRecordAmount",
                "registryRecordImportErrorType", "registryRecordStatus"));

        //Filters
        filterForm.add(new TextField<>("serviceCode"));
        filterForm.add(new TextField<>("personalAccountExt"));
        filterForm.add(new TextField<>("cityType"));
        filterForm.add(new TextField<>("city"));
        filterForm.add(new TextField<>("streetType"));
        filterForm.add(new TextField<>("street"));
        filterForm.add(new TextField<>("buildingNumber"));
        filterForm.add(new TextField<>("buildingCorp"));
        filterForm.add(new TextField<>("apartment"));
        filterForm.add(new TextField<>("room"));

        filterForm.add(new TextField<>("FIO", new Model<String>() {

            @Override
            public String getObject() {
                RegistryRecordData filterObject = filterModel.getObject();
                return StringUtils.join(new String[]{
                        filterObject.getLastName(), filterObject.getFirstName(), filterObject.getMiddleName()
                }, " ");
            }

            @Override
            public void setObject(String fio) {
                RegistryRecord registryRecord = (RegistryRecord)filterModel.getObject();
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
        }));

        filterForm.add(new DatePicker<Date>("operationDate"));

        filterForm.add(new TextField<>("amount"));

        filterForm.add(new DropDownChoice<>("importErrorType",
                Arrays.asList(ImportErrorType.values()),
                new IChoiceRenderer<ImportErrorType>() {
                    @Override
                    public Object getDisplayValue(ImportErrorType type) {
                        return type.getLabel(getLocale());
                    }

                    @Override
                    public String getIdValue(ImportErrorType type, int i) {
                        return type.getId().toString();
                    }
                }
        ).setNullValid(true));

        filterForm.add(new DropDownChoice<>("status",
                Arrays.asList(RegistryRecordStatus.values()),
                new IChoiceRenderer<RegistryRecordStatus>() {
                    @Override
                    public Object getDisplayValue(RegistryRecordStatus type) {
                        return type.getLabel(getLocale());
                    }

                    @Override
                    public String getIdValue(RegistryRecordStatus type, int i) {
                        return type.getId().toString();
                    }
                }
        ).setNullValid(true));

        //Reset Action
        AjaxLink reset = new AjaxLink("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                filterForm.clearInput();
                Long registryId = filterModel.getObject().getRegistryId();

                RegistryRecord filterObject = new RegistryRecord();
                filterObject.setRegistryId(registryId);

                filterModel.setObject(filterObject);

                target.add(container);
            }
        };
        filterForm.add(reset);

        //Submit Action
        AjaxButton submit = new AjaxButton("submit", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                target.add(container);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
            }
        };
        filterForm.add(submit);

        //Navigator
        container.add(new PagingNavigator("navigator", dataView, getPreferencesPage(), container));

        add(new Link("back") {
            @Override
            public void onClick() {
                setResponsePage(RegistryList.class);
            }
        });
    }

    private Class<? extends Page> getEditPage() {
        return FormTemplatePage.class;
    }

    private PageParameters getEditPageParams(Long id) {
        PageParameters parameters = new PageParameters();
        if (id != null) {
            parameters.add("registryRecordId", id);
        }
        return parameters;
    }

    private void showIMessages(AjaxRequestTarget target) {
        if (imessenger.countIMessages() > 0) {
            IMessenger.IMessage importMessage;

            while ((importMessage = imessenger.getNextIMessage()) != null) {
                switch (importMessage.getType()) {
                    case ERROR:
                        container.error(importMessage.getLocalizedString(getLocale()));
                        break;
                    case INFO:
                        container.info(importMessage.getLocalizedString(getLocale()));
                        break;
                }
            }
            target.add(container);
        }
    }

    private class MessageBehavior extends AjaxSelfUpdatingTimerBehavior {
        private MessageBehavior(Duration updateInterval) {
            super(updateInterval);
        }

        @Override
        protected void onPostProcessTarget(AjaxRequestTarget target) {
            showIMessages(target);

            if (finishCallback.isCompleted() && imessenger.countIMessages() <= 0) {
                stop();
                container.remove(timerBehavior);
                timerBehavior = null;
            }
        }
    }
}
