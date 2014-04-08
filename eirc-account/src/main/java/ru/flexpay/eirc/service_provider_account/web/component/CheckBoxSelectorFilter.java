package ru.flexpay.eirc.service_provider_account.web.component;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.AbstractFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxSelector;

/**
 * @author Pavel Sknar
 */
public class CheckBoxSelectorFilter extends AbstractFilter {
    /**
     * @param id   component id
     * @param form
     */
    public CheckBoxSelectorFilter(String id, FilterForm<?> form, CheckBox... checkBox) {
        super(id, form);
        add(new CheckBoxSelector("filter", checkBox));
    }
}
