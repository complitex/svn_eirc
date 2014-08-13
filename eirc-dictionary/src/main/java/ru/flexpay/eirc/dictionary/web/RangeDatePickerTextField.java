package ru.flexpay.eirc.dictionary.web;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.plugins.datepicker.DateRange;
import org.apache.commons.lang.time.DateUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.complitex.dictionary.util.DateUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.flexpay.eirc.dictionary.web.util.DateRangeUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public class RangeDatePickerTextField extends com.googlecode.wicket.jquery.ui.plugins.datepicker.RangeDatePickerTextField {

    private static final DateTimeFormatter AJAX_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";

    private Options options;

    public RangeDatePickerTextField(String id, IModel<DateRange> model) {
        this(id, model, new Options("calendars", 3));
    }

    public RangeDatePickerTextField(String id, IModel<DateRange> model, Options options) {
        super(id, model, options);
        this.options = options;
        setCurrentDate(model.getObject());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new PackageResourceReference(
                RangeDatePickerTextField.class, RangeDatePickerTextField.class.getSimpleName() + ".css")));
    }

    @Override
    public void onValueChanged(AjaxRequestTarget target) {
        super.onValueChanged(target);
        DateRange dateRange = getModelObject();
        if (DateRangeUtil.isChanged(dateRange)) {
            setCurrentDate(dateRange);
        }
    }

    public void setCurrentDate(DateRange dateRange) {
        Date date = dateRange.getEnd().getTime() == DateUtil.MAX_END_DATE.getTime()? new Date() : dateRange.getEnd();
        options.set("current", String.format("new Date('%s')", AJAX_DATE_FORMAT.print(
                DateUtils.addMonths(date, -1).
                        getTime())));
    }

    @Override
    protected DateFormat newDateFormat(Locale locale) {
        DateFormat df = new SimpleDateFormat(DEFAULT_DATE_FORMAT, locale);
        df.setTimeZone(DateRange.UTC);

        return df;
    }
}
