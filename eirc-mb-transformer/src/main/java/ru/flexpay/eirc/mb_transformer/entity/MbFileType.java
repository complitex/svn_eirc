package ru.flexpay.eirc.mb_transformer.entity;

/**
* @author Pavel Sknar
*/
public enum MbFileType {
    CHARGES(".nac"), CORRECTIONS(".kor");


    private String extension;

    MbFileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
