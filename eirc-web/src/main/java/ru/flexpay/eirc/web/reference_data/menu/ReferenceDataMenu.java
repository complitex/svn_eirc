/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.web.reference_data.menu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.wicket.Page;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;

import java.util.List;
import java.util.Locale;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.strategy.StrategyFactory;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.complitex.template.web.security.SecurityRole;

/**
 *
 * @author Artem
 */
@AuthorizeInstantiation(SecurityRole.REFERENCE_DATA_MODULE_VIEW)
public class ReferenceDataMenu extends ResourceTemplateMenu {

    private static final List<String> REFERENCE_DATA_ENTITIES = ImmutableList.of(
            "owner_relationship", "ownership_form", "registration_type", "document_type",
            "military_service_relation", "departure_reason", "housing_rights");

    private static IStrategy getStrategy(String entity) {
        return EjbBeanLocator.getBean(StrategyFactory.class).getStrategy(entity);
    }

    @Override
    public String getTitle(Locale locale) {
        return getString(ReferenceDataMenuResources.class, locale, "reference_data_menu");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        List<ITemplateLink> links = Lists.newArrayList();
        for (final String entity : REFERENCE_DATA_ENTITIES) {
            links.add(new ITemplateLink() {

                @Override
                public String getLabel(Locale locale) {
                    return getStrategy(entity).getPluralEntityLabel(locale);
                }

                @Override
                public Class<? extends Page> getPage() {
                    return getStrategy(entity).getListPage();
                }

                @Override
                public PageParameters getParameters() {
                    return getStrategy(entity).getListPageParams();
                }

                @Override
                public String getTagId() {
                    return entity + "_item";
                }
            });
        }
        return ImmutableList.copyOf(links);
    }

    @Override
    public String getTagId() {
        return "reference_data_menu";
    }
}
