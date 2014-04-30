package ru.flexpay.eirc.payments_communication.entity;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@XmlRootElement(name="quittanceDebtInfo")
public class QuittanceDebtInfo extends ResponseContent {

    private List<QuittanceInfo> quittanceInfo = Lists.newArrayList();

    public QuittanceDebtInfo() {
    }

    public QuittanceDebtInfo(ResponseStatus responseStatus, List<QuittanceInfo> quittanceInfo) {
        super(responseStatus);
        this.quittanceInfo = quittanceInfo;
    }

    public List<QuittanceInfo> getQuittanceInfo() {
        return quittanceInfo;
    }

    public void setQuittanceInfo(List<QuittanceInfo> quittanceInfo) {
        this.quittanceInfo = quittanceInfo;
    }
}
