package ru.flexpay.eirc.service_provider_account.web.edit;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.correction.web.component.AbstractCorrectionEditPanel;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.organization_type.entity.OrganizationType;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccountCorrection;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountCorrectionBean;
import ru.flexpay.eirc.service_provider_account.web.component.ServiceProviderAccountPicker;
import ru.flexpay.eirc.service_provider_account.web.list.ServiceProviderAccountCorrectionList;

import javax.ejb.EJB;
import java.util.List;
import java.util.Locale;

/**
 * @author Anatoly Ivanov java@inheaven.ru
 *         Date: 29.11.13 18:44
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceProviderAccountCorrectionEdit extends FormTemplatePage {
    public static final String CORRECTION_ID = "correction_id";

    @EJB
    private ServiceProviderAccountCorrectionBean serviceProviderAccountCorrectionBean;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    private static final List<Long> ORGANIZATION_TYPES = ImmutableList.of(OrganizationType.PAYMENT_COLLECTOR.getId());

    public ServiceProviderAccountCorrectionEdit(PageParameters params) {
        add(new AbstractCorrectionEditPanel<ServiceProviderAccountCorrection>("service_provider_account_edit_panel",
                params.get(CORRECTION_ID).toOptionalLong()) {

            @Override
            protected List<Long> getOrganizationTypeIds() {
                return ORGANIZATION_TYPES;
            }

            @Override
            protected ServiceProviderAccountCorrection getCorrection(Long correctionId) {
                return serviceProviderAccountCorrectionBean.geServiceProviderAccountCorrection(correctionId);
            }

            @Override
            protected ServiceProviderAccountCorrection newCorrection() {
                return new ServiceProviderAccountCorrection();
            }

            @Override
            protected IModel<String> internalObjectLabel(Locale locale) {
                return new ResourceModel("service_provider_account");
            }

            @Override
            protected ServiceProviderAccountPicker internalObjectPanel(String id) {
                return new ServiceProviderAccountPicker(id, new Model<ServiceProviderAccount>() {
                    @Override
                    public ServiceProviderAccount getObject() {
                        if (getCorrection().getObjectId() != null) {
                            return serviceProviderAccountBean.getServiceProviderAccount(getCorrection().getObjectId());
                        }

                        return null;
                    }

                    @Override
                    public void setObject(ServiceProviderAccount object) {
                        getCorrection().setObjectId(object.getId());
                    }
                });
            }

            @Override
            protected String getNullObjectErrorMessage() {
                return getString("service_provider_account_required");
            }

            @Override
            protected boolean validateExistence() {
                return serviceProviderAccountCorrectionBean.getServiceProviderAccountCorrectionsCount(FilterWrapper.of(getCorrection())) > 0;
            }

            @Override
            protected Class<? extends Page> getBackPageClass() {
                return ServiceProviderAccountCorrectionList.class;
            }

            @Override
            protected PageParameters getBackPageParameters() {
                return new PageParameters();
            }

            @Override
            protected void save() {
                serviceProviderAccountCorrectionBean.save(getCorrection());
            }

            @Override
            protected void delete() {
                serviceProviderAccountCorrectionBean.delete(getCorrection());
            }

            @Override
            protected IModel<String> getTitleModel() {
                return new ResourceModel("title");
            }
        });
    }
}
