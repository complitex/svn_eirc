package ru.flexpay.eirc.payments_communication.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@XmlRootElement(name="debtInfo")
public class DebtInfo extends ResponseContent {

    @XmlElement
    private List<ServiceDetails> serviceDetails;

    public DebtInfo() {
    }

    public DebtInfo(ResponseStatus responseStatus) {
        this(responseStatus, Collections.<ServiceDetails>emptyList());
    }

    public DebtInfo(ResponseStatus responseStatus, List<ServiceDetails> serviceDetails) {
        super(responseStatus);
        this.serviceDetails = serviceDetails;
    }

    public List<ServiceDetails> getServiceDetails() {
        return serviceDetails;
    }

    public void setServiceDetails(List<ServiceDetails> serviceDetails) {
        this.serviceDetails = serviceDetails;
    }
}
