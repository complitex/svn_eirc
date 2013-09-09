package ru.flexpay.eirc.registry.web.list;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.web.component.DatePicker;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink;
import org.complitex.template.web.template.FormTemplatePage;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.ImportErrorType;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.registry.entity.RegistryRecordStatus;
import ru.flexpay.eirc.registry.service.RegistryRecordBean;

import javax.ejb.EJB;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.complitex.dictionary.util.PageUtil.newSorting;

/**
 * @author Pavel Sknar
 */
public class RegistryRecordList extends TemplatePage {

    private static final SimpleDateFormat OPERATION_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    @EJB
    private RegistryRecordBean registryRecordBean;

    private IModel<RegistryRecord> filterModel = new CompoundPropertyModel<>(new RegistryRecord());

    public RegistryRecordList(PageParameters params) throws ExecutionException, InterruptedException {
        StringValue registryIdParam = params.get("registryId");
        if (registryIdParam == null || registryIdParam.isEmpty()) {
            getSession().error(getString("error_registryId_not_found"));
            setResponsePage(RegistryList.class);
            return;
        }
        filterModel.getObject().setRegistryId(registryIdParam.toLong());
        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final FeedbackPanel messages = new FeedbackPanel("messages");
        messages.setOutputMarkupId(true);
        add(messages);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);

        //Form
        final Form<RegistryRecord> filterForm = new Form<>("filterForm", filterModel);
        container.add(filterForm);

        //Data Provider
        final DataProvider<RegistryRecord> dataProvider = new DataProvider<RegistryRecord>() {

            @Override
            protected Iterable<? extends RegistryRecord> getData(int first, int count) {
                FilterWrapper<RegistryRecord> filterWrapper = FilterWrapper.of(filterModel.getObject(), first, count);
                filterWrapper.setAscending(getSort().isAscending());
                filterWrapper.setSortProperty(getSort().getProperty());
                filterWrapper.setLike(true);

                return registryRecordBean.getRegistryRecords(filterWrapper);
            }

            @Override
            protected int getSize() {
                FilterWrapper<RegistryRecord> filterWrapper = FilterWrapper.of(new RegistryRecord());
                return registryRecordBean.count(filterWrapper);
            }
        };
        dataProvider.setSort("registry_record_id", SortOrder.ASCENDING);

        //Data View
        DataView<RegistryRecord> dataView = new DataView<RegistryRecord>("data", dataProvider, 1) {

            @Override
            protected void populateItem(Item<RegistryRecord> item) {
                final RegistryRecord registryRecord = item.getModelObject();

                item.setModel(new CompoundPropertyModel<>(item.getModel()));

                item.add(new Label("serviceCode"));
                item.add(new Label("personalAccountExt"));
                item.add(new Label("townType"));
                item.add(new Label("townName"));
                item.add(new Label("streetType"));
                item.add(new Label("streetName"));
                item.add(new Label("buildingNum"));
                item.add(new Label("buildingBulkNum"));
                item.add(new Label("apartmentNum"));
                item.add(new Label("lastName"));
                item.add(new Label("firstName"));
                item.add(new Label("middleName"));
                item.add(new Label("operationDate", registryRecord.getOperationDate() != null ?
                        OPERATION_DATE_FORMAT.format(registryRecord.getOperationDate()) : ""));
                item.add(new Label("amount"));
                StringBuilder unitContainers = new StringBuilder();
                for (Container registryContainer : registryRecord.getContainers()) {
                    if (unitContainers.length() > 0) {
                        unitContainers.append(";");
                    }
                    unitContainers.append(registryContainer.getData());
                }
                item.add(new Label("containers", unitContainers.toString()));
                item.add(new Label("importErrorType", registryRecord.getImportErrorType() != null?
                        registryRecord.getImportErrorType().getLabel(getLocale()) : ""));
                item.add(new Label("status", registryRecord.getStatus().getLabel(getLocale())));

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
            }
        };
        filterForm.add(dataView);

        //Sorting
        filterForm.add(newSorting("header.", dataProvider, dataView, filterForm, true,
                "registryRecordServiceCode", "registryRecordPersonalAccountExt",
                "registryRecordTownType",    "registryRecordTownName",
                "registryRecordStreetType", "registryRecordStreetName",
                "registryRecordBuildingNumber", "registryRecordBulkNumber",
                "registryRecordApartmentNumber", "registryRecordFio",
                "registryRecordOperationDate", "registryRecordAmount",
                "registryRecordImportErrorType", "registryRecordStatus"));

        //Filters
        filterForm.add(new TextField<>("serviceCode"));
        filterForm.add(new TextField<>("personalAccountExt"));
        filterForm.add(new TextField<>("townType"));
        filterForm.add(new TextField<>("townName"));
        filterForm.add(new TextField<>("streetType"));
        filterForm.add(new TextField<>("streetName"));
        filterForm.add(new TextField<>("buildingNum"));
        filterForm.add(new TextField<>("buildingBulkNum"));
        filterForm.add(new TextField<>("apartmentNum"));

        filterForm.add(new TextField<>("FIO", new Model<String>() {

            @Override
            public String getObject() {
                RegistryRecord filterObject = filterModel.getObject();
                return StringUtils.join(new String[]{
                        filterObject.getLastName(), filterObject.getFirstName(), filterObject.getMiddleName()
                }, " ");
            }

            @Override
            public void setObject(String fio) {
                if (StringUtils.isBlank(fio)) {
                    filterModel.getObject().setLastName(null);
                    filterModel.getObject().setFirstName(null);
                    filterModel.getObject().setMiddleName(null);
                } else {
                    fio = fio.trim();
                    String[] personFio = fio.split(" ", 3);

                    if (personFio.length > 0) {
                        filterModel.getObject().setLastName(personFio[0]);
                    }
                    if (personFio.length > 1) {
                        filterModel.getObject().setFirstName(personFio[1]);
                    } else {
                        filterModel.getObject().setFirstName(null);
                    }
                    if (personFio.length > 2) {
                        filterModel.getObject().setMiddleName(personFio[2]);
                    } else {
                        filterModel.getObject().setMiddleName(null);
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
}
