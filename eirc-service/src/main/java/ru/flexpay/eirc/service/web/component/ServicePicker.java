package ru.flexpay.eirc.service.web.component;

import com.google.common.collect.ImmutableMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.odlabs.wiquery.core.javascript.JsStatement;
import org.odlabs.wiquery.ui.core.JsScopeUiEvent;
import org.odlabs.wiquery.ui.dialog.Dialog;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;

import javax.ejb.EJB;
import java.util.Collections;

/**
 *
 * @author Artem
 */
public class ServicePicker extends FormComponentPanel<Service> {

    private static final TextTemplate CENTER_DIALOG_JS =
            new PackageTextTemplate(ServicePicker.class, "CenterDialog.js");

    @EJB
    private LocaleBean localeBean;
    private boolean showData;
    private final FilterWrapper<Service> filterWrapper;

    @EJB
    private ServiceBean serviceBean;

    @Override
    public void renderHead(IHeaderResponse response) {
        response.renderCSSReference(new PackageResourceReference(
                ServicePicker.class, ServicePicker.class.getSimpleName() + ".css"));
        response.renderJavaScriptReference(new PackageResourceReference(
                ServicePicker.class, ServicePicker.class.getSimpleName() + ".js"));
    }

    public ServicePicker(String id, IModel<Service> model) {
        this(id, model, false, null, true);
    }

    public ServicePicker(String id, IModel<Service> model, boolean required,
                         IModel<String> labelModel, boolean enabled) {
        super(id, model);

        setRequired(required);
        setLabel(labelModel);

        final Label serviceLabel = new Label("serviceLabel",
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        Service service = getModelObject();
                        if (service != null) {
                            return service.getName(localeBean.convert(getLocale()));
                        } else {
                            return getString("service_not_selected");
                        }
                    }
                });
        serviceLabel.setOutputMarkupId(true);
        add(serviceLabel);

        final Dialog lookupDialog = new Dialog("lookupDialog") {

            {
                getOptions().putLiteral("width", "auto");
            }
        };
        lookupDialog.setPosition(Dialog.WindowPosition.CENTER);
        lookupDialog.setModal(true);
        lookupDialog.setOpenEvent(JsScopeUiEvent.quickScope(new JsStatement().self().chain("parents", "'.ui-dialog:first'").
                chain("find", "'.ui-dialog-titlebar-close'").
                chain("hide").render()));
        lookupDialog.setCloseOnEscape(false);
        add(lookupDialog);
        lookupDialog.setVisibilityAllowed(enabled);
        add(lookupDialog);

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupPlaceholderTag(true);
        lookupDialog.add(content);

        final Form<Void> filterForm = new Form<Void>("filterForm");
        content.add(filterForm);

        this.filterWrapper = FilterWrapper.of(new Service());

        final DataProvider<Service> dataProvider = new DataProvider<Service>() {

            @Override
            protected Iterable<? extends Service> getData(int first, int count) {
                if (!showData) {
                    return Collections.emptyList();
                }
                filterWrapper.setLocale(localeBean.convert(getLocale()));
                filterWrapper.setFirst(first);
                filterWrapper.setCount(count);
                filterWrapper.setSortProperty("service_name");
                return serviceBean.getServices(filterWrapper);
            }

            @Override
            protected int getSize() {
                if (!showData) {
                    return 0;
                }
                filterWrapper.setLocale(localeBean.convert(getLocale()));
                return serviceBean.count(filterWrapper);
            }
        };

        filterForm.add(new TextField<String>("nameFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getName(localeBean.convert(getLocale()));
            }

            @Override
            public void setObject(String name) {
                filterWrapper.getObject().addName(localeBean.convert(getLocale()), name);
            }
        }));
        filterForm.add(new TextField<>("codeFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getCode();
            }

            @Override
            public void setObject(String code) {
                filterWrapper.getObject().setCode(code);
            }
        }));

        final IModel<Service> serviceModel = new Model<>();

        final AjaxLink<Void> select = new AjaxLink<Void>("select") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (serviceModel.getObject() == null) {
                    throw new IllegalStateException("Unexpected behaviour.");
                } else {
                    ServicePicker.this.getModel().setObject(serviceModel.getObject());
                    clearAndCloseLookupDialog(serviceModel, target, lookupDialog, content, this);
                    target.add(serviceLabel);
                }
            }
        };
        select.setOutputMarkupPlaceholderTag(true);
        select.setVisible(false);
        content.add(select);

        final RadioGroup<Service> radioGroup = new RadioGroup<>("radioGroup", serviceModel);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                toggleSelectButton(select, target, serviceModel);
            }
        });
        filterForm.add(radioGroup);

        DataView<Service> data = new DataView<Service>("data", dataProvider) {

            @Override
            protected void populateItem(Item<Service> item) {
                final Service service = item.getModelObject();

                item.add(new Radio<>("radio", item.getModel(), radioGroup));
                item.add(new Label("name", service.getName(localeBean.convert(getLocale()))));
                item.add(new Label("code", service.getCode()));
            }
        };
        radioGroup.add(data);

        PagingNavigator pagingNavigator = new PagingNavigator("navigator", data, content) {

            @Override
            public boolean isVisible() {
                return showData;
            }
        };
        pagingNavigator.setOutputMarkupPlaceholderTag(true);
        content.add(pagingNavigator);

        IndicatingAjaxButton find = new IndicatingAjaxButton("find", filterForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                showData = true;
                target.add(content);
                target.appendJavaScript(CENTER_DIALOG_JS.asString(
                        ImmutableMap.of("dialogId", lookupDialog.getMarkupId())));
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
            }
        };
        filterForm.add(find);

        AjaxLink<Void> cancel = new AjaxLink<Void>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearAndCloseLookupDialog(serviceModel, target, lookupDialog, content, select);
            }
        };
        content.add(cancel);

        AjaxLink<Void> choose = new AjaxLink<Void>("choose") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                lookupDialog.open(target);
            }
        };
        choose.setVisibilityAllowed(enabled);
        add(choose);
    }

    private void toggleSelectButton(Component select, AjaxRequestTarget target, IModel<Service> organizationModel) {
        boolean wasVisible = select.isVisible();
        select.setVisible(organizationModel.getObject() != null);
        if (select.isVisible() ^ wasVisible) {
            target.add(select);
        }
    }

    private void clearAndCloseLookupDialog(IModel<Service> organizationModel,
            AjaxRequestTarget target, Dialog lookupDialog, WebMarkupContainer content, Component select) {
        organizationModel.setObject(null);
        select.setVisible(false);
        this.showData = false;
        clearFilter();
        target.add(content);
        lookupDialog.close(target);
    }

    private void clearFilter() {
        filterWrapper.setObject(new Service());
    }

    @Override
    protected void convertInput() {
        setConvertedInput(getModelObject());
    }
}
