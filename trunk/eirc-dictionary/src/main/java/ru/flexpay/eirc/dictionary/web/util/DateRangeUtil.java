package ru.flexpay.eirc.dictionary.web.util;

import com.googlecode.wicket.jquery.ui.plugins.datepicker.DateRange;
import org.apache.commons.lang.time.DateUtils;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.util.DateUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Pavel Sknar
 */
public class DateRangeUtil {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static DateRange getAllDateRange() {
        return new DateRange(DateUtil.MIN_BEGIN_DATE, DateUtil.MAX_END_DATE);
    }

    public static void setDate(FilterWrapper<?> filterWrapper, String key, DateRange dateRange) {
        Object value = dateRange.getStart() == null || dateRange.getEnd() == null ||
                (DateUtils.isSameDay(dateRange.getStart(), DateUtil.MIN_BEGIN_DATE) &&
                        DateUtils.isSameDay(dateRange.getEnd(), DateUtil.MAX_END_DATE))
                ? null :
                new DateRange(
                        DateUtil.getBeginOfDay(dateRange.getStart()),
                        DateUtil.getEndOfDay(dateRange.getEnd())
                );
        if (value != null) {
            filterWrapper.getMap().put(key, value);
        }
    }

    public static boolean isChanged(DateRange dateRange) {
        return !DateUtils.isSameDay(dateRange.getEnd(), DateUtil.MAX_END_DATE);
    }
}
