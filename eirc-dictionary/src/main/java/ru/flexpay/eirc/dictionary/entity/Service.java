package ru.flexpay.eirc.dictionary.entity;

/**
 * @author Pavel Sknar
 */
public class Service extends DictionaryNamedObject {

    private Service parent;

    public Service getParent() {
        return parent;
    }

    public void setParent(Service parent) {
        this.parent = parent;
    }
}
