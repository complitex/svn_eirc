package ru.flexpay.eirc.dictionary.web;

import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.web.AbstractComplexAttributesPanel;
import org.complitex.dictionary.web.component.DomainObjectComponentUtil;
import org.complitex.dictionary.web.component.organization.OrganizationPicker;
import org.complitex.dictionary.web.model.AttributeStringModel;
import org.complitex.dictionary.web.model.LongModel;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceTypeStrategy;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.ejb.EJB;
import java.nio.charset.Charset;

/**
 * @author Pavel Sknar
 */
public class ModuleInstancePrivateKeyPanel extends AbstractComplexAttributesPanel {

    @EJB
    private StringCultureBean stringBean;

    @EJB
    private ModuleInstanceStrategy moduleInstanceStrategy;

    @EJB
    private ModuleInstanceTypeStrategy moduleInstanceTypeStrategy;

    public ModuleInstancePrivateKeyPanel(String id, boolean disabled) {
        super(id, disabled);
    }

    @Override
    protected void init() {
        DomainObject moduleInstance = getDomainObject();

        addPrivateKeyContainer(ModuleInstanceStrategy.PRIVATE_KEY, false, moduleInstance, "privateKeyContainer", new CallbackButton() {
            @Override
            public void onSubmit(AttributeStringModel attributeStringModel) {
                try {
                    //KeyGenerator keyGen = KeyGenerator.getInstance("HmacMD5");
                    //SecretKey key = keyGen.generateKey();
                    // Generate a key for the HMAC-SHA1 keyed-hashing algorithm
                    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
                    SecretKey key = keyGen.generateKey();
                    attributeStringModel.setObject(new String(Base64.encodeBase64(key.getEncoded()), Charset.forName("UTF-8")));
                } catch (Exception ex) {
                    throw new RuntimeException("Inner error", ex);
                }
            }
        });

        addOrganizationContainer(moduleInstance, "organizationContainer");

        addModuleInstanceTypeContainer(moduleInstance, "typeContainer");
    }

    private void addOrganizationContainer(DomainObject moduleInstance, String name) {
        WebMarkupContainer container = new WebMarkupContainer(name);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);

        Attribute attribute = moduleInstance.getAttribute(ModuleInstanceStrategy.ORGANIZATION);

        final EntityAttributeType attributeType =
                moduleInstanceStrategy.getEntity().getAttributeType(ModuleInstanceStrategy.ORGANIZATION);

        container.add(new OrganizationPicker("organization", new LongModel(new AttributeStringModel(attribute)),
                attributeType.isMandatory(), new Model<>(getString("organization")),
                true, OrganizationType.USER_ORGANIZATION.getId()));

        container.add(new Label("label",
                DomainObjectComponentUtil.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));
    }

    private void addModuleInstanceTypeContainer(final DomainObject moduleInstance, String name) {
        WebMarkupContainer container = new WebMarkupContainer(name);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);

        final Attribute attribute = moduleInstance.getAttribute(ModuleInstanceStrategy.MODULE_INSTANCE_TYPE);
        //final SimpleTypeModel<Integer> model = new SimpleTypeModel<>(attribute, new IntegerConverter());
        final Model<DomainObject> model = new Model<DomainObject>(new DomainObject(attribute.getValueId())) {
            @Override
            public void setObject(DomainObject object) {
                super.setObject(object);
                attribute.setValueId(object.getId());
            }
        };
        container.add(
            new DropDownChoice<>("type",
                    model,
                    moduleInstanceTypeStrategy.getAll(),
                    new IChoiceRenderer<DomainObject>() {
                        @Override
                        public Object getDisplayValue(DomainObject type) {
                            return moduleInstanceTypeStrategy.displayDomainObject(type, getLocale());
                        }

                        @Override
                        public String getIdValue(DomainObject type, int i) {
                            return type != null && type.getId() != null ? type.getId().toString(): "-1";
                        }
                    }
            )
        );

        final EntityAttributeType attributeType =
                moduleInstanceStrategy.getEntity().getAttributeType(ModuleInstanceStrategy.MODULE_INSTANCE_TYPE);
        container.add(new Label("label",
                DomainObjectComponentUtil.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));
    }

    private WebMarkupContainer addPrivateKeyContainer(final long attributeTypeId, boolean disabled,
                                                      DomainObject moduleInstance, String name, final CallbackButton callbackButton) {
        WebMarkupContainer container = new WebMarkupContainer(name);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);
        Attribute attribute = moduleInstance.getAttribute(attributeTypeId);
        if (attribute == null) {
            attribute = new Attribute();
            attribute.setAttributeTypeId(attributeTypeId);
            attribute.setObjectId(moduleInstance.getId());
            attribute.setAttributeId(1L);
            attribute.setLocalizedValues(stringBean.newStringCultures());
        }
        final AttributeStringModel attributeModel = new AttributeStringModel(attribute);
        final EntityAttributeType attributeType =
                moduleInstanceStrategy.getEntity().getAttributeType(attributeTypeId);
        container.add(new Label("label",
                DomainObjectComponentUtil.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));

        final Component key =
                DomainObjectComponentUtil.newInputComponent("module_instance", getStrategyName(),
                        moduleInstance, attribute, getLocale(), disabled);
        key.setOutputMarkupId(true);
        container.add(key);

        container.add(
                new AjaxButton("update") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        callbackButton.onSubmit(attributeModel);
                        target.add(key);
                    }
                }
        );

        return container;
    }

    protected String getStrategyName() {
        return null;
    }

    public static interface CallbackButton {
        void onSubmit(AttributeStringModel attributeStringModel);
    }
}

