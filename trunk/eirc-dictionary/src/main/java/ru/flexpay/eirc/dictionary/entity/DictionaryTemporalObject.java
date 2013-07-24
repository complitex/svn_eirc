package ru.flexpay.eirc.dictionary.entity;

import java.util.Date;

/**
 * Object with time history.
 *
 * @author Pavel Sknar
 */
public abstract class DictionaryTemporalObject extends DictionaryObject {

    /**
     * Each object different pkId.
     */
    private Long pkId;

    /**
     * One object with same id can have different by disjoint begin date and end date.
     */
    private Date beginDate;
    private Date endDate;

    public Long getPkId() {
        return pkId;
    }

    public void setPkId(Long pkId) {
        this.pkId = pkId;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
