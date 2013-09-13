package ru.flexpay.eirc.registry.entity;

import ru.flexpay.eirc.dictionary.entity.DictionaryObject;

/**
 * @author Pavel Sknar
 */
public class Container extends DictionaryObject {

    /**
     * Symbol used escape special symbols
     */
    public static final char ESCAPE_SYMBOL = '\\';

    /**
     * Symbol used to split fields in containers
     */
    public static final char CONTAINER_DATA_DELIMITER = ':';

    public static final long CONTAINER_DATA_MAX_SIZE = 2048;

    private String data;
    private ContainerType type;
    private Long parentId;

    public Container() {
    }

    public Container(String data, ContainerType type) {
        this.data = data;
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
