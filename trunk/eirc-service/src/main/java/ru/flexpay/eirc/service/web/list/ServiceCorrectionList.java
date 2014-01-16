package ru.flexpay.eirc.service.web.list;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.correction.web.AbstractCorrectionList;
import org.complitex.dictionary.entity.Correction;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.template.web.security.SecurityRole;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.entity.ServiceCorrection;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;
import ru.flexpay.eirc.service.web.edit.ServiceCorrectionEdit;

import javax.ejb.EJB;
import java.util.List;


/**
 * @author Anatoly A. Ivanov java@inheaven.ru
 *         Date: 28.11.13 15:36
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceCorrectionList extends AbstractCorrectionList<ServiceCorrection> {
    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private LocaleBean localeBean;

    @EJB(name = IOrganizationStrategy.BEAN_NAME, beanInterface = IOrganizationStrategy.class)
    private EircOrganizationStrategy organizationStrategy;

    public ServiceCorrectionList() {
        super("organization");
    }

    @Override
    protected ServiceCorrection newCorrection() {
        return new ServiceCorrection();
    }

    @Override
    protected List<ServiceCorrection> getCorrections(FilterWrapper<ServiceCorrection> filterWrapper) {
        return serviceCorrectionBean.getServiceCorrections(filterWrapper);
    }

    @Override
    protected Integer getCorrectionsCount(FilterWrapper<ServiceCorrection> filterWrapper) {
        return serviceCorrectionBean.getServiceCorrectionsCount(filterWrapper);
    }

    @Override
    protected Class<? extends WebPage> getEditPage() {
        return ServiceCorrectionEdit.class;
    }

    @Override
    protected PageParameters getEditPageParams(Long objectCorrectionId) {
        PageParameters parameters = new PageParameters();

        if (objectCorrectionId != null) {
            parameters.set(ServiceCorrectionEdit.CORRECTION_ID, objectCorrectionId);
        }

        return parameters;
    }

    @Override
    protected IModel<String> getTitleModel() {
        return new ResourceModel("title");
    }

    @Override
    protected String displayInternalObject(Correction correction) {
        Service service = serviceBean.getService(correction.getObjectId());
        return service == null? "" : service.getName(localeBean.convert(getLocale()));
    }

    @Override
    protected IModel<List<DomainObject>> getAllOuterOrganizationsModel() {
        return new LoadableDetachableModel<List<DomainObject>>() {

            @Override
            protected List<DomainObject> load() {
                return organizationStrategy.getAllPaymentCollectors(getLocale());
            }
        };
    }

    @Override
    protected IModel<List<DomainObject>> getAllUserOrganizationsModel() {
        return new LoadableDetachableModel<List<DomainObject>>() {

            @Override
            protected List<DomainObject> load() {
                return organizationStrategy.getAllServiceProviders(getLocale());
            }
        };
    }
}
