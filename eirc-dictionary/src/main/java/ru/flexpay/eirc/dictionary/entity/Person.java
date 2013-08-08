package ru.flexpay.eirc.dictionary.entity;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author Pavel Sknar
 */
public class Person implements Serializable {
    private String firstName;
    private String lastName;
    private String middleName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    private void appendNotEmptyField(StringBuilder builder, String field) {
        if (StringUtils.isNotEmpty(field)) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(field);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendNotEmptyField(builder, lastName);
        appendNotEmptyField(builder, firstName);
        appendNotEmptyField(builder, middleName);
        return builder.toString();
    }
}
