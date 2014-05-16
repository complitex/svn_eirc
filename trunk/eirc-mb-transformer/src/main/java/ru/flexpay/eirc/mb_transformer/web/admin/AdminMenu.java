package ru.flexpay.eirc.mb_transformer.web.admin;

import com.google.common.collect.Lists;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.admin.web.AdminTemplateMenu;
import org.complitex.template.web.pages.ConfigEdit;
import org.complitex.template.web.template.ITemplateLink;

import java.util.List;
import java.util.Locale;

/**
 * @author Artem
 */
public class AdminMenu extends AdminTemplateMenu {

    @Override
    public List<ITemplateLink> getTemplateLinks(Locale locale) {
        List<ITemplateLink> links = Lists.newArrayList();

        links.add(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(AdminTemplateMenu.class, locale, "template_menu.user_list");
            }

            @Override
            public Class<? extends Page> getPage() {
                return UserList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return "UserList";
            }
        });

        links.add(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(ConfigEdit.class, locale, "title");
            }

            @Override
            public Class<? extends Page> getPage() {
                return MbTransformerConfigEdit.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return "ConfigEdit";
            }
        });


        return links;
    }
}
