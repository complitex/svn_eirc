package ru.flexpay.eirc.mb_transformer.entity;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
* @author Pavel Sknar
*/
public class MbFile {
    private String fileName;
    private String shortName;
    private MbFileType fileType;
    private long fileLength;
    private InputStream inputStream;

    public MbFile(String dir, String fileName) throws FileNotFoundException {
        File mbFile = new File(dir, fileName);

        this.fileName = fileName;

        inputStream = new FileInputStream(mbFile);
        fileLength = mbFile.length();
        fileType = fileName.endsWith(MbFileType.CHARGES.getExtension()) ? MbFileType.CHARGES : MbFileType.CORRECTIONS;
        shortName = StringUtils.removeEnd(fileName, fileType.getExtension());
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public MbFileType getFileType() {
        return fileType;
    }

    public String getShortName() {
        return shortName;
    }
}
