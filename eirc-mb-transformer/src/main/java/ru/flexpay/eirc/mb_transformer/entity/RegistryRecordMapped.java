package ru.flexpay.eirc.mb_transformer.entity;

import ru.flexpay.eirc.mb_transformer.service.MbConverterException;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;

import java.util.Date;

/**
 * @author Pavel Sknar
 */
public abstract class RegistryRecordMapped implements RegistryRecordData {

    private String[] fields;
    private String serviceCode;
    private Date modificationDate;

    private boolean using = false;

    protected RegistryRecordMapped(String[] fields, String serviceCode) throws MbConverterException {
        initData(fields, serviceCode);
    }

    public void initData(String[] fields, String serviceCode) throws MbConverterException, MbConverterException {
        this.fields = fields;
        this.serviceCode = serviceCode;
        using = true;
    }

    public boolean isUsing() {
        return using;
    }

    public void setNotUsing() {
        this.using = false;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public String getServiceCode() {
        return serviceCode;
    }

    public String getField(int idx) {
        return fields[idx];
    }
}