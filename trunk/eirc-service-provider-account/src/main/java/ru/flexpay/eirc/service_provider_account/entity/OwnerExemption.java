package ru.flexpay.eirc.service_provider_account.entity;

import org.apache.commons.lang.builder.ToStringBuilder;
import ru.flexpay.eirc.dictionary.entity.DictionaryTemporalObject;
import ru.flexpay.eirc.dictionary.entity.Person;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class OwnerExemption extends DictionaryTemporalObject {
    private Long serviceProviderAccountId;
    private Person person;
    private String inn;

    private List<Exemption> exemptions;

    public Long getServiceProviderAccountId() {
        return serviceProviderAccountId;
    }

    public void setServiceProviderAccountId(Long serviceProviderAccountId) {
        this.serviceProviderAccountId = serviceProviderAccountId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public List<Exemption> getExemptions() {
        return exemptions;
    }

    public void setExemptions(List<Exemption> exemptions) {
        this.exemptions = exemptions;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("serviceProviderAccountId", serviceProviderAccountId)
                .append("person", person)
                .append("inn", inn)
                .append("exemptions", exemptions)
                .toString();
    }
}
