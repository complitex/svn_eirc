package ru.flexpay.eirc.service_provider_account.web.component;

import com.google.common.collect.ImmutableMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
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
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import java.util.Collections;

/**
 *
 * @author Artem
 */
public class ServiceProviderAccountPicker extends FormComponentPanel<ServiceProviderAccount> {

    private static final TextTemplate CENTER_DIALOG_JS =
            new PackageTextTemplate(ServiceProviderAccountPicker.class, "CenterDialog.js");

    @EJB
    private LocaleBean localeBean;
    private boolean showData;
    private final FilterWrapper<ServiceProviderAccount> filterWrapper;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new PackageResourceReference(
                ServiceProviderAccountPicker.class, ServiceProviderAccountPicker.class.getSimpleName() + ".css")));
        response.render(JavaScriptHeaderItem.forReference(new PackageResourceReference(
                ServiceProviderAccountPicker.class, ServiceProviderAccountPicker.class.getSimpleName() + ".js")));
    }

    public ServiceProviderAccountPicker(String id, IModel<ServiceProviderAccount> model) {
        this(id, model, false, null, true);
    }

    public ServiceProviderAccountPicker(String id, IModel<ServiceProviderAccount> model, boolean required,
                                        IModel<String> labelModel, boolean enabled) {
        super(id, model);

        setRequired(required);
        setLabel(labelModel);

        final Label serviceLabel = new Label("serviceProviderAccountLabel",
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        ServiceProviderAccount serviceProviderAccount = getModelObject();
                        if (serviceProviderAccount != null) {
                            return serviceProviderAccount.getAccountNumber();
                        } else {
                            return getString("service_provider_account_not_selected");
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

        this.filterWrapper = FilterWrapper.of(new ServiceProviderAccount());
        this.filterWrapper.getObject().setEircAccount(new EircAccount());
        this.filterWrapper.getObject().setService(new Service());

        final DataProvider<ServiceProviderAccount> dataProvider = new DataProvider<ServiceProviderAccount>() {

            @Override
            protected Iterable<? extends ServiceProviderAccount> getData(long first, long count) {
                if (!showData) {
                    return Collections.emptyList();
                }
                filterWrapper.setLocale(localeBean.convert(getLocale()));
                filterWrapper.setFirst(first);
                filterWrapper.setCount(count);
                filterWrapper.setSortProperty("spa_account_number");
                return serviceProviderAccountBean.getServiceProviderAccounts(filterWrapper);
            }

            @Override
            protected int getSize() {
                if (!showData) {
                    return 0;
                }
                filterWrapper.setLocale(localeBean.convert(getLocale()));
                return serviceProviderAccountBean.count(filterWrapper);
            }
        };

        filterForm.add(new TextField<>("serviceProviderAccountNumberFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getAccountNumber();
            }

            @Override
            public void setObject(String name) {
                filterWrapper.getObject().setAccountNumber(name);
            }
        }));

        filterForm.add(new TextField<>("eircAccountNumberFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getEircAccount().getAccountNumber();
            }

            @Override
            public void setObject(String name) {
                filterWrapper.getObject().getEircAccount().setAccountNumber(name);
            }
        }));

        filterForm.add(new TextField<>("serviceNameFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getService().getName(localeBean.convert(getLocale()));
            }

            @Override
            public void setObject(String name) {
                filterWrapper.getObject().getService().addName(localeBean.convert(getLocale()), name);
            }
        }));

        filterForm.add(new TextField<>("serviceProviderNameFilter", new Model<String>() {

            @Override
            public String getObject() {
                return filterWrapper.getObject().getOrganizationName();
            }

            @Override
            public void setObject(String name) {
                filterWrapper.getObject().setOrganizationName(name);
            }
        }));

        final IModel<ServiceProviderAccount> serviceModel = new Model<>();

        final AjaxLink<Void> select = new AjaxLink<Void>("select") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (serviceModel.getObject() == null) {
                    throw new IllegalStateException("Unexpected behaviour.");
                } else {
                    ServiceProviderAccountPicker.this.getModel().setObject(serviceModel.getObject());
                    clearAndCloseLookupDialog(serviceModel, target, lookupDialog, content, this);
                    target.add(serviceLabel);
                }
            }
        };
        select.setOutputMarkupPlaceholderTag(true);
        select.setVisible(false);
        content.add(select);

        final RadioGroup<ServiceProviderAccount> radioGroup = new RadioGroup<>("radioGroup", serviceModel);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                toggleSelectButton(select, target, serviceModel);
            }
        });
        filterForm.add(radioGroup);

        DataView<ServiceProviderAccount> data = new DataView<ServiceProviderAccount>("data", dataProvider) {

            @Override
            protected void populateItem(Item<ServiceProviderAccount> item) {
                final ServiceProviderAccount serviceProviderAccount = item.getModelObject();

                item.add(new Radio<>("radio", item.getModel(), radioGroup));
                item.add(new Label("spaAccountNumber", serviceProviderAccount.getAccountNumber()));
                item.add(new Label("eircAccountNumber", serviceProviderAccount.getEircAccount().getAccountNumber()));
                item.add(new Label("serviceName", serviceProviderAccount.getService().getName(localeBean.convert(getLocale()))));
                item.add(new Label("serviceProviderName", serviceProviderAccount.getOrganizationName()));
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

    private void toggleSelectButton(Component select, AjaxRequestTarget target, IModel<ServiceProviderAccount> organizationModel) {
        boolean wasVisible = select.isVisible();
        select.setVisible(organizationModel.getObject() != null);
        if (select.isVisible() ^ wasVisible) {
            target.add(select);
        }
    }

    private void clearAndCloseLookupDialog(IModel<ServiceProviderAccount> organizationModel,
            AjaxRequestTarget target, Dialog lookupDialog, WebMarkupContainer content, Component select) {
        organizationModel.setObject(null);
        select.setVisible(false);
        this.showData = false;
        clearFilter();
        target.add(content);
        lookupDialog.close(target);
    }

    private void clearFilter() {
        filterWrapper.setObject(new ServiceProviderAccount());
        filterWrapper.getObject().setEircAccount(new EircAccount());
        filterWrapper.getObject().setService(new Service());
    }

    @Override
    protected void convertInput() {
        setConvertedInput(getModelObject());
    }
}
