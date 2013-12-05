package ru.flexpay.eirc.service.entity;

import ru.flexpay.eirc.dictionary.entity.DictionaryNamedObject;

/**
 * @author Pavel Sknar
 */
public class Service extends DictionaryNamedObject {

    private String code;
    private Long parentId;

    public Service() {
    }

    public Service(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
