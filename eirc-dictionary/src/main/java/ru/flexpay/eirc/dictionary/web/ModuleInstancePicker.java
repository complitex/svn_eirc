package ru.flexpay.eirc.dictionary.web;

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
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.example.AttributeExample;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.complitex.dictionary.web.component.paging.PagingNavigator;
import org.odlabs.wiquery.core.javascript.JsStatement;
import org.odlabs.wiquery.ui.core.JsScopeUiEvent;
import org.odlabs.wiquery.ui.dialog.Dialog;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;

import javax.ejb.EJB;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy.*;

/**
 *
 * @author Artem
 */
public class ModuleInstancePicker extends FormComponentPanel<DomainObject> {

    private static final TextTemplate CENTER_DIALOG_JS =
            new PackageTextTemplate(ModuleInstancePicker.class, "CenterDialog.js");

    @EJB
    private LocaleBean localeBean;
    private boolean showData = false; //todo RadioGroup bug on showData = true
    private DomainObjectExample example;

    @EJB
    protected ModuleInstanceStrategy moduleInstanceStrategy;

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new PackageResourceReference(
                ModuleInstancePicker.class, ModuleInstancePicker.class.getSimpleName() + ".css")));
        response.render(JavaScriptHeaderItem.forReference(new PackageResourceReference(
                ModuleInstancePicker.class, ModuleInstancePicker.class.getSimpleName() + ".js")));
    }

    public ModuleInstancePicker(String id) {
        this(id, null, false, null, true);
    }

    public ModuleInstancePicker(String id, IModel<String> model, Long... type) {
        this(id, model, false, null, true, type);
    }

    public ModuleInstancePicker(String id, IModel<String> model, boolean required,
                                IModel<String> labelModel, boolean enabled, Long... type) {
        super(id);
        setModel(new ModuleInstanceModel(model));
        init(required, labelModel, enabled, Arrays.asList(type));
    }

    private void init(boolean required,
                      IModel<String> labelModel, boolean enabled, List<Long> types) {

        setRequired(required);
        setLabel(labelModel);

        final Label moduleInstanceLabel = new Label("moduleInstanceLabel",
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        DomainObject moduleInstance = getModelObject();
                        if (moduleInstance != null) {
                            return moduleInstanceStrategy.displayDomainObject(moduleInstance, getLocale());
                        } else {
                            return getString("module_instance_not_selected");
                        }
                    }
                });
        moduleInstanceLabel.setOutputMarkupId(true);
        add(moduleInstanceLabel);

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

        example = newExample(types);

        final DataProvider<DomainObject> dataProvider = new DataProvider<DomainObject>() {

            @Override
            protected Iterable<? extends DomainObject> getData(long first, long count) {
                if (!showData) {
                    return Collections.emptyList();
                }
                example.setLocaleId(localeBean.convert(getLocale()).getId());
                example.setStart(first);
                example.setSize(count);
                return moduleInstanceStrategy.find(example);
            }

            @Override
            protected int getSize() {
                if (!showData) {
                    return 0;
                }
                example.setLocaleId(localeBean.convert(getLocale()).getId());
                return moduleInstanceStrategy.count(example);
            }
        };

        filterForm.add(new TextField<>("nameFilter", new Model<String>() {

            @Override
            public String getObject() {
                return example.getAttributeExample(NAME).getValue();
            }

            @Override
            public void setObject(String name) {
                example.getAttributeExample(NAME).setValue(name);
            }
        }));
        filterForm.add(new TextField<>("indexFilter", new Model<String>() {

            @Override
            public String getObject() {
                return example.getAttributeExample(UNIQUE_INDEX).getValue();
            }

            @Override
            public void setObject(String code) {
                example.getAttributeExample(UNIQUE_INDEX).setValue(code);
            }
        }));

        final IModel<DomainObject> moduleInstanceModel = new Model<>();

        final AjaxLink<Void> select = new AjaxLink<Void>("select") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (moduleInstanceModel.getObject() == null) {
                    throw new IllegalStateException("Unexpected behaviour.");
                } else {
                    ModuleInstancePicker.this.getModel().setObject(moduleInstanceModel.getObject());
                    clearAndCloseLookupDialog(moduleInstanceModel, target, lookupDialog, content, this);
                    target.add(moduleInstanceLabel);
                }
            }
        };
        select.setOutputMarkupPlaceholderTag(true);
        select.setVisible(false);
        content.add(select);

        final RadioGroup<DomainObject> radioGroup = new RadioGroup<DomainObject>("radioGroup", moduleInstanceModel);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                toggleSelectButton(select, target, moduleInstanceModel);
            }
        });
        filterForm.add(radioGroup);

        DataView<DomainObject> data = new DataView<DomainObject>("data", dataProvider) {

            @Override
            protected void populateItem(Item<DomainObject> item) {
                final DomainObject moduleInstance = item.getModelObject();

                item.add(new Radio<>("radio", item.getModel(), radioGroup));
                item.add(new Label("name", AttributeUtil.getStringCultureValue(moduleInstance, NAME, getLocale())));
                item.add(new Label("index", AttributeUtil.getStringValue(moduleInstance, UNIQUE_INDEX)));
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
                clearAndCloseLookupDialog(moduleInstanceModel, target, lookupDialog, content, select);
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

    private void toggleSelectButton(Component select, AjaxRequestTarget target, IModel<DomainObject> moduleInstanceModel) {
        boolean wasVisible = select.isVisible();
        select.setVisible(moduleInstanceModel.getObject() != null);
        if (select.isVisible() ^ wasVisible) {
            target.add(select);
        }
    }

    private void clearAndCloseLookupDialog(IModel<DomainObject> moduleInstanceModel,
            AjaxRequestTarget target, Dialog lookupDialog, WebMarkupContainer content, Component select) {
        moduleInstanceModel.setObject(null);
        select.setVisible(false);
        this.showData = false;
        clearExample();
        target.add(content);
        lookupDialog.close(target);
    }

    private DomainObjectExample newExample(List<Long> types) {
        DomainObjectExample e = new DomainObjectExample();
        e.addAttributeExample(new AttributeExample(NAME));
        e.addAttributeExample(new AttributeExample(UNIQUE_INDEX));

        if (types != null && !types.isEmpty()) {
            e.addAdditionalParam(MODULE_INSTANCE_TYPE_PARAMETER, types);
        }
        return e;
    }

    private void clearExample() {
        example.getAttributeExample(NAME).setValue(null);
        example.getAttributeExample(UNIQUE_INDEX).setValue(null);
    }

    @Override
    protected void convertInput() {
        setConvertedInput(getModelObject());
    }

    private class ModuleInstanceModel extends Model<DomainObject> {

        private IModel<String> model;

        public ModuleInstanceModel(IModel<String> model) {
            this.model = model;
        }

        @Override
        public DomainObject getObject() {
            return model.getObject() == null? null : moduleInstanceStrategy.findById(Long.valueOf(model.getObject()), true);
        }

        @Override
        public void setObject(DomainObject object) {
            super.setObject(object);
            if (object != null) {
                model.setObject(String.valueOf(object.getId()));
            } else {
                model.setObject(null);
            }
        }
    }
}
