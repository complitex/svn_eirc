package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author Pavel Sknar
 */
public class ScrollBookmarkablePageLink<T> extends Panel {
    public <C extends Page> ScrollBookmarkablePageLink(String id, Class<C> pageClass, PageParameters parameters,
                                                       String markupId, IModel<String> linkLabelModel) {
        super(id);

        org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink link =
                new org.complitex.dictionary.web.component.scroll.ScrollBookmarkablePageLink<>("link", pageClass, parameters, markupId);
        add(link);

        link.add(new Label("label", linkLabelModel));
    }
}
