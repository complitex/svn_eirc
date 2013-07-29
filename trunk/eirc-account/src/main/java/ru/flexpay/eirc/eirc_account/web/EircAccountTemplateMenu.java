package ru.flexpay.eirc.eirc_account.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.organization.web.CommonResources;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.eirc_account.strategy.EircAccountStrategy;

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
    private EircAccountStrategy strategy;

    protected EircAccountStrategy getStrategy() {
        return strategy;
    }

    @Override
    public String getTitle(Locale locale) {
        return getString(CommonResources.class, locale, "organization_menu");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        List<ITemplateLink> links = ImmutableList.<ITemplateLink>of(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getStrategy().getPluralEntityLabel(locale);
            }

            @Override
            public Class<? extends Page> getPage() {
                return getStrategy().getListPage();
            }

            @Override
            public PageParameters getParameters() {
                return getStrategy().getListPageParams();
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