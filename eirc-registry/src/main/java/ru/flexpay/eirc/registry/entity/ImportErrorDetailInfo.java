package ru.flexpay.eirc.registry.entity;

import java.io.Serializable;

/**
 * @author Pavel Sknar
 */
public class ImportErrorDetailInfo implements Serializable {
    private Long count;
    private ImportErrorType importErrorType;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public ImportErrorType getImportErrorType() {
        return importErrorType;
    }

    public void setImportErrorType(ImportErrorType importErrorType) {
        this.importErrorType = importErrorType;
    }
}
