/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.registry.entity;

import java.io.Serializable;

/**
 *
 * @author Artem
 */
public class StatusDetailInfo implements Serializable {

    private Long count;
    private RegistryRecordStatus status;
    private boolean importErrorExist;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public boolean isImportErrorExist() {
        return importErrorExist;
    }

    public void setImportErrorExist(boolean importErrorExist) {
        this.importErrorExist = importErrorExist;
    }

    public RegistryRecordStatus getStatus() {
        return status;
    }

    public void setStatus(RegistryRecordStatus status) {
        this.status = status;
    }
}
