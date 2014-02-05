package ru.flexpay.eirc.mb_transformer.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.mb_transformer.web.registry.EircPaymentsTransformer;
import ru.flexpay.eirc.mb_transformer.web.registry.MbCorrectionsTransformer;

import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class MbTransformerTemplateMenu extends ResourceTemplateMenu {

    @Override
    public String getTitle(Locale locale) {
        return getString(MbTransformerTemplateMenu.class, locale, "title");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(final Locale locale) {
        return ImmutableList.of(new ITemplateLink() {

                                    @Override
                                    public String getLabel(Locale locale) {
                                        return getString(MbTransformerTemplateMenu.class, locale, "simple_saldo");
                                    }

                                    @Override
                                    public Class<? extends Page> getPage() {
                                        return MbCorrectionsTransformer.class;
                                    }

                                    @Override
                                    public PageParameters getParameters() {
                                        return new PageParameters();
                                    }

                                    @Override
                                    public String getTagId() {
                                        return "simple_saldo_item";
                                    }
                                }, new ITemplateLink() {

                                    @Override
                                    public String getLabel(Locale locale) {
                                        return getString(MbTransformerTemplateMenu.class, locale, "payments");
                                    }

                                    @Override
                                    public Class<? extends Page> getPage() {
                                        return EircPaymentsTransformer.class;
                                    }

                                    @Override
                                    public PageParameters getParameters() {
                                        return new PageParameters();
                                    }

                                    @Override
                                    public String getTagId() {
                                        return "payments_item";
                                    }
                                }
        );
    }

    @Override
    public String getTagId() {
        return "registry_menu";
    }
}
