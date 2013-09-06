package ru.flexpay.eirc.registry.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.registry.web.list.RegistryList;

import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class RegistryTemplateMenu extends ResourceTemplateMenu {

    public static final String EIRC_REGISTRY_MENU_ITEM = "eirc_registry_item";

    @Override
    public String getTitle(Locale locale) {
        return getString(RegistryTemplateMenu.class, locale, "eirc_registry_menu");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        List<ITemplateLink> links = ImmutableList.<ITemplateLink>of(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(RegistryTemplateMenu.class, locale, "eirc_registry_menu");
            }

            @Override
            public Class<? extends Page> getPage() {
                return RegistryList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return EIRC_REGISTRY_MENU_ITEM;
            }
        });
        return links;
    }

    @Override
    public String getTagId() {
        return "eirc_registry_menu";
    }
}
