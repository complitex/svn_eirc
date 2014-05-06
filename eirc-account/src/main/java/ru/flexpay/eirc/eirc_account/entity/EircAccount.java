package ru.flexpay.eirc.eirc_account.entity;

import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.DictionaryTemporalObject;
import ru.flexpay.eirc.dictionary.entity.Person;

/**
 * @author Pavel Sknar
 */
public class EircAccount extends DictionaryTemporalObject {

    private String accountNumber;

    private Address address;

    private Person person;

    public EircAccount() {
    }

    public EircAccount(Long objectId) {
        setId(objectId);
    }

    public EircAccount(Address address) {
        this.address = address;
    }

    public EircAccount(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
