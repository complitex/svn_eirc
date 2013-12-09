package ru.flexpay.eirc.registry.service.handle.exchange;

/**
 * @author Pavel Sknar
 */
public class OperationResult<T> {

    private T oldObject;

    private T newObject;

    private Long code;

    public OperationResult(T oldObject, T newObject, Long code) {
        this.oldObject = oldObject;
        this.newObject = newObject;
        this.code = code;
    }

    public T getOldObject() {
        return oldObject;
    }

    public void setOldObject(T oldObject) {
        this.oldObject = oldObject;
    }

    public T getNewObject() {
        return newObject;
    }

    public void setNewObject(T newObject) {
        this.newObject = newObject;
    }

    public Long getCode() {
        return code;
    }

    public void setCode(Long code) {
        this.code = code;
    }
}
