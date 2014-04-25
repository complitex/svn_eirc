package ru.flexpay.eirc.organization.web.edit;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.DomainObjectComponentUtil;
import org.complitex.organization.strategy.web.edit.OrganizationEditComponent;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;

import javax.ejb.EJB;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Artem
 */
public class EircOrganizationEditComponent extends OrganizationEditComponent {
    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    @EJB
    private StringCultureBean stringBean;

    private WebMarkupContainer emailContainer;
    private WebMarkupContainer serviceContainer;

    private List<Attribute> services = null;

    public EircOrganizationEditComponent(String id, boolean disabled) {
        super(id, disabled);
    }

    @Override
    protected Organization getDomainObject() {
        return (Organization) super.getDomainObject();
    }

    @Override
    protected void init() {
        super.init();

        final boolean isDisabled = isDisabled();

        final Organization organization = getDomainObject();

        // General attributes.
        {
            for (Map.Entry<Long, String> attribute : EircOrganizationStrategy.GENERAL_ATTRIBUTE_TYPES.entrySet()) {
                addAttributeContainer(attribute.getKey(), isDisabled, organization, attribute.getValue() + "Container");
            }
        }

        //E-mail. It is service provider organization only attribute.
        {
            emailContainer = addAttributeContainer(EircOrganizationStrategy.EMAIL, isDisabled, organization, "emailContainer");

            //initial visibility
            emailContainer.setVisible(isServiceProvider());
        }

        //Services. It is service provider organization only attribute.
        {
            serviceContainer = addServiceContainer(organization, "serviceContainer");

            //initial visibility
            serviceContainer.setVisible(isServiceProvider());
        }
    }

    private WebMarkupContainer addAttributeContainer(final long attributeTypeId, boolean disabled,
                                                     Organization organization, String name) {
        WebMarkupContainer container = new WebMarkupContainer(name);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);
        Attribute attribute = organization.getAttribute(attributeTypeId);
        if (attribute == null) {
            attribute = new Attribute();
            attribute.setAttributeTypeId(attributeTypeId);
            attribute.setObjectId(organization.getId());
            attribute.setAttributeId(1L);
            attribute.setLocalizedValues(stringBean.newStringCultures());
        }
        final EntityAttributeType attributeType =
                organizationStrategy.getEntity().getAttributeType(attributeTypeId);
        container.add(new Label("label",
                DomainObjectComponentUtil.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));

        container.add(
                DomainObjectComponentUtil.newInputComponent("organization", getStrategyName(),
                        organization, attribute, getLocale(), disabled));

        return container;
    }

    private WebMarkupContainer addServiceContainer(Organization organization, String name) {
        Long attributeTypeId = EircOrganizationStrategy.SERVICE;

        WebMarkupContainer container = new WebMarkupContainer(name);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);
        Attribute attribute = organization.getAttribute(attributeTypeId);
        if (attribute == null) {
            attribute = new Attribute();
            attribute.setAttributeTypeId(attributeTypeId);
            attribute.setObjectId(organization.getId());
            attribute.setAttributeId(1L);
            attribute.setLocalizedValues(stringBean.newStringCultures());
        }
        final EntityAttributeType attributeType =
                organizationStrategy.getEntity().getAttributeType(attributeTypeId);
        container.add(new Label("label",
                DomainObjectComponentUtil.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));

        ServiceAllowableListPanel panel = new ServiceAllowableListPanel("input", organization);
        services = panel.getServices();
        container.add(panel);

        return container;
    }

    @Override
    protected void onOrganizationTypeChanged(AjaxRequestTarget target) {
        super.onOrganizationTypeChanged(target);

        //e-mail.
        {
            boolean emailContainerWasVisible = emailContainer.isVisible();
            emailContainer.setVisible(isServiceProvider());
            boolean emailContainerVisibleNow = emailContainer.isVisible();
            if (emailContainerWasVisible ^ emailContainerVisibleNow) {
                target.add(emailContainer);
            }
        }
        
        // allowable services
        {
            boolean serviceContainerWasVisible = serviceContainer.isVisible();
            serviceContainer.setVisible(isServiceProvider());
            boolean serviceContainerVisibleNow = serviceContainer.isVisible();
            if (serviceContainerWasVisible ^ serviceContainerVisibleNow) {
                target.add(serviceContainer);
            }
        }
    }

    public boolean isServiceProvider() {
        List<DomainObject> types = getOrganizationTypesModel().getObject();
        for (DomainObject type : types) {
            if (type.getId() != null &&
                    type.getId().equals(OrganizationType.SERVICE_PROVIDER.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserOrganization() {
        return super.isUserOrganization();
    }

    @Override
    protected boolean isDistrictVisible() {
        return super.isDistrictVisible() || isServiceProvider();
    }

    @Override
    protected void onPersist() {
        super.onPersist();

        final DomainObject organization = getDomainObject();

        organization.removeAttribute(EircOrganizationStrategy.SERVICE);
        if (!isServiceProvider()) {
            organization.removeAttribute(EircOrganizationStrategy.EMAIL);
        } else if (services != null) {
            long attributeId = 1;
            for (Attribute service : services) {
                service.setAttributeId(attributeId++);
                organization.addAttribute(service);
            }
        }
    }

    @Override
    protected String getStrategyName() {
        return EircOrganizationStrategy.EIRC_ORGANIZATION_STRATEGY_NAME;
    }
}
