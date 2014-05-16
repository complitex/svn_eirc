package ru.flexpay.eirc.mb_transformer.web.admin;

import org.complitex.template.web.template.TemplatePage;

/**
 * @author Pavel Sknar
 */
public class UserList extends org.complitex.admin.web.UserList {
    @Override
    protected boolean isUsingAddress() {
        return false;
    }

    @Override
    protected Class<? extends TemplatePage> getEditPageClass() {
        return UserEdit.class;
    }
}
