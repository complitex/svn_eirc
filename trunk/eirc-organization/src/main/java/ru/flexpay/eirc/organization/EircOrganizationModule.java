package ru.flexpay.eirc.organization;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.organization.DefaultOrganizationModule;
import org.complitex.organization.IOrganizationModule;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton(name = DefaultOrganizationModule.CUSTOM_ORGANIZATION_MODULE_BEAN_NAME)
@Startup
public class EircOrganizationModule implements IOrganizationModule {

    @EJB
    private EircOrganizationStrategy eircOrganizationStrategy;

    public static final String NAME = "ru.flexpay.eirc.organization";

    @Override
    public Class<? extends WebPage> getEditPage() {
        return eircOrganizationStrategy.getEditPage();
    }

    @Override
    public PageParameters getEditPageParams() {
        return eircOrganizationStrategy.getEditPageParams(null, null, null);
    }
}
