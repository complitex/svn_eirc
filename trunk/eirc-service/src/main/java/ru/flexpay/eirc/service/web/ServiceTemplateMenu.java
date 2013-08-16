package ru.flexpay.eirc.service.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.web.list.ServiceList;

import javax.ejb.EJB;
import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class ServiceTemplateMenu extends ResourceTemplateMenu {

    public static final String EIRC_SERVICE_MENU_ITEM = "eirc_service_item";

    @EJB
    private ServiceBean serviceBean;

    protected ServiceBean getServiceBean() {
        return serviceBean;
    }

    @Override
    public String getTitle(Locale locale) {
        return getString(ServiceTemplateMenu.class, locale, "eirc_service_menu");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        List<ITemplateLink> links = ImmutableList.<ITemplateLink>of(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(ServiceTemplateMenu.class, locale, "eirc_service_menu");
            }

            @Override
            public Class<? extends Page> getPage() {
                return ServiceList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return EIRC_SERVICE_MENU_ITEM;
            }
        });
        return links;
    }

    @Override
    public String getTagId() {
        return "eirc_account_menu";
    }
}
