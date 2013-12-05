package ru.flexpay.eirc.registry.web.list;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.time.Duration;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.AjaxFeedbackPanel;
import org.complitex.dictionary.web.component.DatePicker;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.image.StaticImage;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.UploadButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import org.slf4j.Logger;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryStatus;
import ru.flexpay.eirc.registry.entity.RegistryType;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.RegistryBean;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.link.RegistryLinker;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;
import ru.flexpay.eirc.registry.service.parse.RegistryParser;
import ru.flexpay.eirc.registry.service.parse.RegistryWorkflowManager;

import javax.ejb.EJB;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.complitex.dictionary.util.PageUtil.newSorting;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class RegistryList extends TemplatePage {

    private final Logger log = getLogger(RegistryList.class);

    private static final SimpleDateFormat CREATE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat LOAD_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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

    private WebMarkupContainer container;

    @EJB
    private RegistryMessenger imessenger;

    private IModel<Registry> filterModel = new CompoundPropertyModel<>(new Registry());

    @EJB
    private RegistryFinishCallback finishCallback;

    @EJB
    private RegistryWorkflowManager registryWorkflowManager;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    public RegistryList() throws ExecutionException, InterruptedException {
        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);
        container.add(messages);


        if (imessenger.countIMessages() > 0 || !finishCallback.isCompleted()) {
            timerBehavior = new MessageBehavior(Duration.seconds(5));
            this.container.add(timerBehavior);
        }


        //Form
        final Form<Registry> filterForm = new Form<>("filterForm", filterModel);
        container.add(filterForm);

        //Data Provider
        final DataProvider<Registry> dataProvider = new DataProvider<Registry>() {

            @Override
            protected Iterable<? extends Registry> getData(int first, int count) {
                FilterWrapper<Registry> filterWrapper = FilterWrapper.of(filterModel.getObject(), first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setLike(true);

                return registryBean.getRegistries(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<Registry> filterWrapper = FilterWrapper.of(new Registry());
                return registryBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("creation_date", SortOrder.ASCENDING);

        //Data View
        DataView<Registry> dataView = new DataView<Registry>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<Registry> item) {
                final Registry registry = item.getModelObject();

                Organization senderOrganization = organizationStrategy.findById(registry.getSenderOrganizationId(), false);
                Organization recipientOrganization = organizationStrategy.findById(registry.getRecipientOrganizationId(), false);

                item.add(new Label("creationDate", registry.getCreationDate() != null ? CREATE_DATE_FORMAT.format(registry.getCreationDate()) : ""));
                item.add(new Label("sender", organizationStrategy.displayDomainObject(senderOrganization, getLocale())));
                item.add(new Label("recipient", organizationStrategy.displayDomainObject(recipientOrganization, getLocale())));
                item.add(new Label("type", registry.getType().getLabel(getLocale())));
                item.add(new Label("loadDate", registry.getLoadDate() != null ? LOAD_DATE_FORMAT.format(registry.getLoadDate()) : ""));
                item.add(new Label("recordsCount", String.valueOf(registry.getRecordsCount())));
                item.add(new Label("status", registry.getStatus().getLabel(getLocale())));

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
                        return !finishCallback.isCompleted() &&
                                registryWorkflowManager.isLinking(registry) ||
                                registryWorkflowManager.isProcessing(registry);
                    }
                });

                AjaxLink actionLink = new AjaxLink("actionLink") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (registryWorkflowManager.canLink(registry)) {

                            if (timerBehavior == null) {

                                timerBehavior = new MessageBehavior(Duration.seconds(5));

                                RegistryList.this.container.add(timerBehavior);
                            }

                            try {
                                linker.link(registry.getId(), imessenger, finishCallback);
                            } finally {
                                showIMessages(target);
                            }

                        } else if (registryWorkflowManager.canProcess(registry)) {
                            // TODO process registry
                        }
                    }

                    @Override
                    public boolean isVisible() {
                        return registryWorkflowManager.canLink(registry) || registryWorkflowManager.canProcess(registry);
                    }
                };
                actionLink.add(new Label("actionMessage", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (registryWorkflowManager.canLink(registry)) {
                            return getString("link");
                        } else if (registryWorkflowManager.canProcess(registry)) {
                            return getString("process");
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
        filterForm.add(new DatePicker<Date>("creationDate"));
        filterForm.add(new TextField<>("registryNumber"));
        filterForm.add(new DropDownChoice<>("senderOrganization",
                new IModel<DomainObject>() {

                    @Override
                    public DomainObject getObject() {
                        Long organizationId = filterModel.getObject().getSenderOrganizationId();
                        return organizationId != null ?
                                organizationStrategy.findById(organizationId, false) :
                                null;
                    }

                    @Override
                    public void setObject(DomainObject domainObject) {
                        if (domainObject != null) {
                            filterModel.getObject().setSenderOrganizationId(domainObject.getId());
                        } else {
                            filterModel.getObject().setSenderOrganizationId(null);
                        }
                    }

                    @Override
                    public void detach() {

                    }
                },
                organizationStrategy.getAllOuterOrganizations(getLocale()),
                new IChoiceRenderer<DomainObject>() {
                    @Override
                    public Object getDisplayValue(DomainObject organization) {
                        return organizationStrategy.displayDomainObject(organization, getLocale());
                    }

                    @Override
                    public String getIdValue(DomainObject organization, int i) {
                        return organization.getId().toString();
                    }
                }
        ).setNullValid(true));

        filterForm.add(new DropDownChoice<>("recipientOrganization",
                new IModel<DomainObject>() {

                    @Override
                    public DomainObject getObject() {
                        Long organizationId = filterModel.getObject().getRecipientOrganizationId();
                        return organizationId != null ?
                                organizationStrategy.findById(organizationId, false) :
                                null;
                    }

                    @Override
                    public void setObject(DomainObject domainObject) {
                        if (domainObject != null) {
                            filterModel.getObject().setRecipientOrganizationId(domainObject.getId());
                        } else {
                            filterModel.getObject().setRecipientOrganizationId(null);
                        }
                    }

                    @Override
                    public void detach() {

                    }
                },
                organizationStrategy.getAllOuterOrganizations(getLocale()),
                new IChoiceRenderer<DomainObject>() {
                    @Override
                    public Object getDisplayValue(DomainObject organization) {
                        return organizationStrategy.displayDomainObject(organization, getLocale());
                    }

                    @Override
                    public String getIdValue(DomainObject organization, int i) {
                        return organization.getId().toString();
                    }
                }
        ).setNullValid(true));

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

        filterForm.add(new DatePicker<Date>("loadDate"));

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

        return ImmutableList.of(new UploadButton(id, true) {

            @Override
            protected void onClick(final AjaxRequestTarget target) {

                if (timerBehavior == null) {

                    timerBehavior = new MessageBehavior(Duration.seconds(5));

                    container.add(timerBehavior);
                }

                try {
                    parser.parse(imessenger, finishCallback);
                } catch (ExecuteException e) {
                    log.error("Failed parse", e);
                } finally {
                    showIMessages(target);
                }
            }
        });
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
