package ru.flexpay.eirc.registry.web.list;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.googlecode.wicket.jquery.ui.plugins.datepicker.DateRange;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.complitex.dictionary.entity.DictionaryConfig;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.image.StaticImage;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.template.web.component.toolbar.DeleteItemButton;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.UploadButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.dictionary.web.RangeDatePickerTextField;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryStatus;
import ru.flexpay.eirc.registry.entity.RegistryType;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.handle.RegistryHandler;
import ru.flexpay.eirc.registry.service.link.RegistryLinker;
import ru.flexpay.eirc.registry.service.parse.RegistryParser;
import ru.flexpay.eirc.registry.web.component.BrowserFilesDialog;
import ru.flexpay.eirc.registry.web.component.IMessengerContainer;

import javax.ejb.EJB;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.complitex.dictionary.util.PageUtil.newSorting;
import static ru.flexpay.eirc.dictionary.web.util.DateRangeUtil.getAllDateRange;
import static ru.flexpay.eirc.dictionary.web.util.DateRangeUtil.setDate;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class RegistryList extends TemplatePage {

    private static final SimpleDateFormat CREATE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat LOAD_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final String IMAGE_AJAX_LOADER = "images/ajax-loader2.gif";

    @EJB
    private RegistryBean registryBean;

    @EJB
    private ConfigBean configBean;

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    @EJB
    private RegistryParser parser;

    @EJB
    private RegistryLinker linker;

    @EJB
    private RegistryHandler handler;

    private IMessengerContainer container;

    @EJB
    private RegistryMessenger imessengerService;

    private AbstractMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallbackService;

    private AbstractFinishCallback finishCallback;

    @EJB
    private RegistryWorkflowManager registryWorkflowManager;

    @EJB
    private JobProcessor processor;

    @EJB
    private CanceledProcessing canceledProcessing;

    BrowserFilesDialog fileDialog;

    private Map<Registry, AjaxCheckBox> selected = Maps.newHashMap();

    public RegistryList() throws ExecutionException, InterruptedException {
        imessenger = imessengerService.getInstance();
        finishCallback = finishCallbackService.getInstance();

        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);

        container = new IMessengerContainer("container", imessenger, finishCallbackService);
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);
        container.add(messages);

        fileDialog = new BrowserFilesDialog("fileDialog", container, new Model<File>(), configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true)) {
            @Override
            public void onSelected(AjaxRequestTarget target) {
                super.onSelected(target);

                initTimerBehavior();

                try {
                    parser.parse(imessenger, finishCallback, getSelectedFile().getParent(), getSelectedFile().getName(), null);
                } finally {
                    showIMessages(target);
                }
            }
        };
        add(fileDialog);

        //models
        final IModel<Registry> filterModel = new CompoundPropertyModel<>(new Registry());
        final IModel<DateRange> creationDateModel = new Model<>(getAllDateRange());
        final IModel<DateRange> loadDateModel = new Model<>(getAllDateRange());

        //Form
        final Form<Registry> filterForm = new Form<>("filterForm", filterModel);
        container.add(filterForm);


        //Data Provider
        final DataProvider<Registry> dataProvider = new DataProvider<Registry>() {

            @Override
            protected Iterable<? extends Registry> getData(long first, long count) {
                FilterWrapper<Registry> filterWrapper = FilterWrapper.of(filterModel.getObject(), first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setLike(true);

                setDate(filterWrapper, RegistryBean.CREATION_DATE_RANGE, creationDateModel.getObject());
                setDate(filterWrapper, RegistryBean.LOAD_DATE_RANGE, loadDateModel.getObject());

                return registryBean.getRegistries(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<Registry> filterWrapper = FilterWrapper.of(new Registry());
                filterWrapper.setLike(true);

                setDate(filterWrapper, RegistryBean.CREATION_DATE_RANGE, creationDateModel.getObject());
                setDate(filterWrapper, RegistryBean.LOAD_DATE_RANGE, loadDateModel.getObject());

                return registryBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("creation_date", SortOrder.ASCENDING);

        final AjaxCheckBox selectAll;
        filterForm.add(selectAll = new AjaxCheckBox("selectAll", new Model<>(false)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Map.Entry<Registry, AjaxCheckBox> entry : selected.entrySet()) {
                    if (!isExecuting(entry.getKey())) {
                        IModel<Boolean> model = entry.getValue().getModel();
                        model.setObject(getModelObject());
                        target.add(entry.getValue());
                    }
                }
            }
        });

        //Data View
        DataView<Registry> dataView = new DataView<Registry>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<Registry> item) {
                final Registry registry = item.getModelObject();

                AjaxCheckBox select = new AjaxCheckBox("select", new Model<>(false)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (!getModelObject() && selectAll.getModelObject()) {
                            selectAll.setModelObject(false);
                            target.add(selectAll);
                        }
                    }
                };
                select.setVisible(!isExecuting(registry));
                selected.put(registry, select);
                item.add(select);

                Organization senderOrganization = organizationStrategy.findById(registry.getSenderOrganizationId(), false);
                Organization recipientOrganization = organizationStrategy.findById(registry.getRecipientOrganizationId(), false);

                item.add(new Label("creationDate", registry.getCreationDate() != null ? CREATE_DATE_FORMAT.format(registry.getCreationDate()) : ""));
                item.add(new Label("sender", senderOrganization == null ? "" : organizationStrategy.displayDomainObject(senderOrganization, getLocale())));
                item.add(new Label("recipient", recipientOrganization == null ? "" : organizationStrategy.displayDomainObject(recipientOrganization, getLocale())));
                item.add(new Label("type", registry.getType().getLabel(getLocale())));
                item.add(new Label("loadDate", registry.getLoadDate() != null ? LOAD_DATE_FORMAT.format(registry.getLoadDate()) : ""));
                item.add(new Label("recordsCount", String.valueOf(registry.getRecordsCount())));
                item.add(new Label("status", registry.getStatus() == null? getString("deleted") : registry.getStatus().getLabel(getLocale())));

                ScrollBookmarkablePageLink<WebPage> detailsLink = new ScrollBookmarkablePageLink<>("detailsLink",
                        getViewPage(), getViewPageParams(registry.getId()),
                        String.valueOf(registry.getId()));
                detailsLink.add(new Label("registryNumber", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return String.valueOf(registry.getRegistryNumber());
                    }
                }));
                item.add(detailsLink);

                //Анимация в обработке
                item.add(new StaticImage("processing", new SharedResourceReference(IMAGE_AJAX_LOADER)) {

                    @Override
                    public boolean isVisible() {
                        return isExecuting(registry);
                    }
                });

                AjaxLink actionLink = new AjaxLink("actionLink") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (registryWorkflowManager.canLink(registry)) {

                            initTimerBehavior();

                            try {
                                linker.link(registry.getId(), imessenger, finishCallback);
                            } finally {
                                showIMessages(target);
                            }

                        } else if (registryWorkflowManager.canProcess(registry)) {

                            initTimerBehavior();

                            try {
                                handler.handle(registry.getId(), imessenger, finishCallback);
                            } finally {
                                showIMessages(target);
                            }
                        } else if (canCanceled(registry)) {
                            canceledProcessing.cancel(registry.getId());
                        }
                    }

                    @Override
                    public boolean isVisible() {
                        return registryWorkflowManager.canLink(registry) || registryWorkflowManager.canProcess(registry) ||
                                canCanceled(registry);
                    }
                };
                actionLink.add(new Label("actionMessage", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (registryWorkflowManager.canLink(registry)) {
                            return getString("link");
                        } else if (registryWorkflowManager.canProcess(registry)) {
                            return getString("process");
                        } else if (canCanceled(registry)) {
                            return getString("cancel");
                        }
                        return "";
                    }
                }));
                item.add(actionLink);
            }
        };
        filterForm.add(dataView);

        //Sorting
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true,
                "registryCreationDate", "registryNumber", "registrySenderOrganizationId", "registryRecipientOrganizationId", "registryType",
                "registryLoadDate", "registryRecordsCount", "registryStatus"));

        //Filters
        filterForm.add(new RangeDatePickerTextField("creationDate", creationDateModel));
        filterForm.add(new TextField<>("registryNumber"));
        filterForm.add(new OrganizationPicker("senderOrganizationId", filterModel));
        filterForm.add(new OrganizationPicker("recipientOrganizationId", filterModel));

        filterForm.add(new DropDownChoice<>("type",
                Arrays.asList(RegistryType.values()),
                new IChoiceRenderer<RegistryType>() {
                    @Override
                    public Object getDisplayValue(RegistryType type) {
                        return type.getLabel(getLocale());
                    }

                    @Override
                    public String getIdValue(RegistryType type, int i) {
                        return type.getId().toString();
                    }
                }
        ).setNullValid(true));

        filterForm.add(new RangeDatePickerTextField("loadDate", loadDateModel));

        filterForm.add(new DropDownChoice<>("status",
                Arrays.asList(RegistryStatus.values()),
                new IChoiceRenderer<RegistryStatus>() {
                    @Override
                    public Object getDisplayValue(RegistryStatus type) {
                        return type.getLabel(getLocale());
                    }

                    @Override
                    public String getIdValue(RegistryStatus type, int i) {
                        return type.getId().toString();
                    }
                }
        ).setNullValid(true));

        //Reset Action
        AjaxLink reset = new AjaxLink("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                filterForm.clearInput();
                filterModel.setObject(new Registry());
                creationDateModel.setObject(getAllDateRange());
                loadDateModel.setObject(getAllDateRange());

                selected.clear();

                target.add(container);
            }
        };
        filterForm.add(reset);

        //Submit Action
        AjaxButton submit = new AjaxButton("submit", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
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

    public boolean canCanceled(Registry registry) {
        return finishCallbackService.canCanceled(registry.getId()) && !canceledProcessing.isCanceling(registry.getId());
    }

    public boolean isExecuting(Registry registry) {
        return !finishCallbackService.isCompleted(registry.getId()) && registryWorkflowManager.isInWork(registry);
    }

    private Class<? extends Page> getViewPage() {
        return RegistryRecordList.class;
    }

    private PageParameters getViewPageParams(Long id) {
        PageParameters parameters = new PageParameters();
        if (id != null) {
            parameters.add("registryId", id);
        }
        return parameters;
    }

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {

        return ImmutableList.of(
                new UploadButton(id, true) {

                    @Override
                    protected void onClick(final AjaxRequestTarget target) {
                        fileDialog.open(target);
                    }
                },
                new DeleteItemButton(id, true) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        initTimerBehavior();
                        for (final Map.Entry<Registry, AjaxCheckBox> entry : selected.entrySet()) {
                            if (entry.getValue().getModelObject()) {
                                finishCallback.init();
                                processor.processJob(new AbstractJob<Void>() {
                                    @Override
                                    public Void execute() throws ExecuteException {
                                        try {
                                            Registry registry = entry.getKey();

                                            RegistryStatus oldStatus = registry.getStatus();
                                            registry.setStatus(null);
                                            registryBean.updateInNewTransaction(registry);

                                            try {
                                                registryBean.delete(registry);
                                            } catch (Throwable th) {
                                                registry.setStatus(oldStatus);
                                                registryBean.updateInNewTransaction(registry);
                                            }
                                        } finally {
                                            finishCallback.complete();
                                        }
                                        return null;
                                    }
                                });
                            }
                        }
                        selected.clear();
                        target.add(container);
                    }
                }
        );
    }

    private void initTimerBehavior() {
        container.initTimerBehavior();
    }

    private void showIMessages(AjaxRequestTarget target) {
        container.showIMessages(target);
    }


}
