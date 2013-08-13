package ru.flexpay.eirc.eirc_account.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.eirc_account.web.list.EircAccountList;

import javax.ejb.EJB;
import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircAccountTemplateMenu extends ResourceTemplateMenu {

    public static final String EIRC_ACCOUNT_MENU_ITEM = "eirc_account_item";

    @EJB
    private EircAccountBean strategy;

    protected EircAccountBean getStrategy() {
        return strategy;
    }

    @Override
    public String getTitle(Locale locale) {
        return getString(EircAccountTemplateMenu.class, locale, "eirc_account_menu");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        List<ITemplateLink> links = ImmutableList.<ITemplateLink>of(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(EircAccountTemplateMenu.class, locale, "eirc_account_menu");
            }

            @Override
            public Class<? extends Page> getPage() {
                return EircAccountList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return EIRC_ACCOUNT_MENU_ITEM;
            }
        });
        return links;
    }

    @Override
    public String getTagId() {
        return "eirc_account_menu";
    }
}