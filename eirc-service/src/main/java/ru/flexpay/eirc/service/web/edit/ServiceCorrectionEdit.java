package ru.flexpay.eirc.service.web.edit;

import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.correction.web.component.AbstractCorrectionEditPanel;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.FormTemplatePage;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.entity.ServiceCorrection;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;
import ru.flexpay.eirc.service.web.component.ServicePicker;
import ru.flexpay.eirc.service.web.list.ServiceCorrectionList;

import javax.ejb.EJB;
import java.util.Locale;

/**
 * @author Anatoly Ivanov java@inheaven.ru
 *         Date: 29.11.13 18:44
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceCorrectionEdit extends FormTemplatePage {
    public static final String CORRECTION_ID = "correction_id";

    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ServiceBean serviceBean;

    public ServiceCorrectionEdit(PageParameters params) {
        add(new AbstractCorrectionEditPanel<ServiceCorrection>("service_edit_panel",
                params.get(CORRECTION_ID).toOptionalLong()) {

            @Override
            protected ServiceCorrection getCorrection(Long correctionId) {
                return serviceCorrectionBean.geServiceCorrection(correctionId);
            }

            @Override
            protected ServiceCorrection newCorrection() {
                return new ServiceCorrection();
            }

            @Override
            protected IModel<String> internalObjectLabel(Locale locale) {
                return new ResourceModel("service");
            }

            @Override
            protected ServicePicker internalObjectPanel(String id) {
                return new ServicePicker(id, new Model<Service>() {
                    @Override
                    public Service getObject() {
                        if (getCorrection().getObjectId() != null) {
                            return serviceBean.getService(getCorrection().getObjectId());
                        }

                        return null;
                    }

                    @Override
                    public void setObject(Service object) {
                        getCorrection().setObjectId(object.getId());
                    }
                });
            }

            @Override
            protected String getNullObjectErrorMessage() {
                return getString("service_required");
            }

            @Override
            protected boolean validateExistence() {
                return serviceCorrectionBean.getServiceCorrectionsCount(FilterWrapper.of(getCorrection())) > 0;
            }

            @Override
            protected Class<? extends Page> getBackPageClass() {
                return ServiceCorrectionList.class;
            }

            @Override
            protected PageParameters getBackPageParameters() {
                return new PageParameters();
            }

            @Override
            protected void save() {
                serviceCorrectionBean.save(getCorrection());
            }

            @Override
            protected void delete() {
                serviceCorrectionBean.delete(getCorrection());
            }

            @Override
            protected IModel<String> getTitleModel() {
                return new ResourceModel("title");
            }
        });
    }
}
