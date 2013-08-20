package ru.flexpay.eirc.service_provider_account.entity;

import ru.flexpay.eirc.dictionary.entity.DictionaryTemporalObject;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.service.entity.Service;

/**
 * @author Pavel Sknar
 */
public class ServiceProviderAccount extends DictionaryTemporalObject {

    private String accountNumber;

    private EircAccount eircAccount;

    private Long organizationId;

    private Service service;

    private Person person;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public EircAccount getEircAccount() {
        return eircAccount;
    }

    public void setEircAccount(EircAccount eircAccount) {
        this.eircAccount = eircAccount;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
