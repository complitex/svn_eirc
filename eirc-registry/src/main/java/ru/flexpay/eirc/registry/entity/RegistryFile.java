package ru.flexpay.eirc.registry.entity;

import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

import java.util.Date;

/**
 * @author Pavel Sknar
 */
public class RegistryFile extends DictionaryObject {

    private String nameOnServer;
    private String originalName;
    private String userName;
    private String description;
    private Long size = 0L;
    private Date creationDate = new Date();

    public String getNameOnServer() {
        return nameOnServer;
    }

    public void setNameOnServer(String nameOnServer) {
        this.nameOnServer = nameOnServer;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
