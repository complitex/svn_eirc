package ru.flexpay.eirc.mb_transformer.web.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.template.web.template.TemplatePage;

/**
 * @author Pavel Sknar
 */
public class UserEdit extends org.complitex.admin.web.UserEdit {

    public UserEdit() {
        super();
    }

    public UserEdit(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected Class<? extends TemplatePage> getPageListClass() {
        return UserList.class;
    }
}
