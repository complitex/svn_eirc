package ru.flexpay.eirc.organization.strategy.web.edit;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.web.component.DomainObjectInputPanel;
import org.complitex.organization.strategy.web.edit.OrganizationEditComponent;
import ru.flexpay.eirc.dictionary.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.organization.strategy.entity.EircOrganization;
import ru.flexpay.eirc.organization_type.strategy.EircOrganizationTypeStrategy;

import javax.ejb.EJB;
import java.util.Map;

/**
 * 
 * @author Artem
 */
public class EircOrganizationEditComponent extends OrganizationEditComponent {
    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy eircOrganizationStrategy;

    @EJB
    private StringCultureBean stringBean;
    private WebMarkupContainer emailContainer;

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
                eircOrganizationStrategy.getEntity().getAttributeType(attributeTypeId);
        container.add(new Label("label",
                DomainObjectInputPanel.labelModel(attributeType.getAttributeNames(), getLocale())));
        container.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));

        container.add(
                DomainObjectInputPanel.newInputComponent("organization", getStrategyName(),
                        organization, attribute, getLocale(), disabled));

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
    }

    public boolean isServiceProvider() {
        return getDomainObject().isServiceProvider();
    }

    @Override
    public boolean isUserOrganization() {
        return super.isUserOrganization();
    }

    @Override
    protected boolean isDistrictRequired() {
        return isServiceProvider();
    }

    @Override
    protected boolean isDistrictVisible() {
        return super.isDistrictVisible() || isServiceProvider();
    }

    @Override
    protected void onPersist() {
        super.onPersist();

        final DomainObject organization = getDomainObject();

        if (!isServiceProvider()) {
            organization.removeAttribute(EircOrganizationStrategy.EMAIL);
        }
    }

    @Override
    protected String getStrategyName() {
        return EircOrganizationStrategy.EIRC_ORGANIZATION_STRATEGY_NAME;
    }

    @Override
    protected boolean isOrganizationTypeEnabled() {
        Long organizationId = getDomainObject().getId();
        return !(organizationId != null && (organizationId.equals(EircOrganizationStrategy.MODULE_ID)))
                && super.isOrganizationTypeEnabled();
    }
}
