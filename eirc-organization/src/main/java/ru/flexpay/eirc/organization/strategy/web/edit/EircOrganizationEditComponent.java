package ru.flexpay.eirc.organization.strategy.web.edit;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.complitex.dictionary.converter.StringConverter;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.web.component.DisableAwareDropDownChoice;
import org.complitex.dictionary.web.component.DomainObjectInputPanel;
import org.complitex.dictionary.web.component.IDisableAwareChoiceRenderer;
import org.complitex.organization.strategy.web.edit.OrganizationEditComponent;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.organization.strategy.entity.EircOrganization;
import ru.flexpay.eirc.organization.strategy.entity.RemoteDataSource;
import ru.flexpay.eirc.organization_type.strategy.EircOrganizationTypeStrategy;

import javax.ejb.EJB;
import java.util.List;

/**
 * 
 * @author Artem
 */
public class EircOrganizationEditComponent extends OrganizationEditComponent {
    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy eircOrganizationStrategy;

    @EJB
    private StringCultureBean stringBean;

    private WebMarkupContainer dataSourceContainer;
    private WebMarkupContainer emailContainer;
    private IModel<RemoteDataSource> dataSourceModel;

    public EircOrganizationEditComponent(String id, boolean disabled) {
        super(id, disabled);
    }

    @Override
    protected EircOrganization getDomainObject() {
        return (EircOrganization) super.getDomainObject();
    }

    @Override
    protected void init() {
        super.init();

        final boolean isDisabled = isDisabled();

        final EircOrganization organization = getDomainObject();

        //E-mail. It is service provider organization only attribute.
        {
            emailContainer = new WebMarkupContainer("emailContainer");
            emailContainer.setOutputMarkupPlaceholderTag(true);
            add(emailContainer);
            final long attributeTypeId = EircOrganizationStrategy.EMAIL;
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
            emailContainer.add(new Label("label",
                    DomainObjectInputPanel.labelModel(attributeType.getAttributeNames(), getLocale())));
            emailContainer.add(new WebMarkupContainer("required").setVisible(attributeType.isMandatory()));

            emailContainer.add(
                    DomainObjectInputPanel.newInputComponent("organization", getStrategyName(),
                            organization, attribute, getLocale(), isDisabled));

            //initial visibility
            emailContainer.setVisible(isServiceProvider());
        }
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
        for (DomainObject organizationType : getOrganizationTypesModel().getObject()) {
            if (organizationType.getId().equals(EircOrganizationTypeStrategy.SERVICE_PROVIDER)) {
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
