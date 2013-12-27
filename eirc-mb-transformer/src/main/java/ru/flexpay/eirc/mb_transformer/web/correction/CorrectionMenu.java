package ru.flexpay.eirc.mb_transformer.web.correction;

import com.google.common.collect.Lists;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.correction.web.OrganizationCorrectionList;
import org.complitex.template.web.template.ITemplateLink;
import org.complitex.template.web.template.ResourceTemplateMenu;
import ru.flexpay.eirc.service.web.list.ServiceCorrectionList;

import java.util.List;
import java.util.Locale;

/**
 *
 * @author Pavel Sknar
 */
public class CorrectionMenu extends ResourceTemplateMenu {

    @Override
    public String getTitle(Locale locale) {
        return getString(CorrectionMenu.class, locale, "title");
    }

    @Override
    public List<ITemplateLink> getTemplateLinks(Locale locale) {
        List<ITemplateLink> links = Lists.newArrayList();

        links.add(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(CorrectionMenu.class, locale, "organization");
            }

            @Override
            public Class<? extends Page> getPage() {
                return OrganizationCorrectionList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return "organization_correction_item";
            }
        });

        links.add(new ITemplateLink() {

            @Override
            public String getLabel(Locale locale) {
                return getString(CorrectionMenu.class, locale, "service_correction");
            }

            @Override
            public Class<? extends Page> getPage() {
                return ServiceCorrectionList.class;
            }

            @Override
            public PageParameters getParameters() {
                return new PageParameters();
            }

            @Override
            public String getTagId() {
                return "service_correction_item";
            }
        });

        return links;
    }

    @Override
    public String getTagId() {
        return "correction_menu";
    }
}
