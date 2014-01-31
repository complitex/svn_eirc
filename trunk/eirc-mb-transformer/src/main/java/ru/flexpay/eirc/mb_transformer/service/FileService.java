package ru.flexpay.eirc.mb_transformer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.mb_transformer.entity.MbFile;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @EJB(name = "MbTransformerConfigBean")
    private MbTransformerConfigBean configBean;

    public List<String> getFileList() {
        String dir = getWorkDir();
        if (dir == null) {
            return Collections.emptyList();
        }
        String[] files = new File(dir).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return true;//name.toLowerCase().contains("." + extension);
            }
        });
        return Arrays.asList(files);
    }

    public String getWorkDir() {
        String workDir = configBean.getString(MbTransformerConfig.WORK_DIR, true);
        if (!isDirectory(workDir)) {
            log.error("Is not directory {}={}", MbTransformerConfig.WORK_DIR.name(), workDir);
            return null;
        }
        return workDir;
    }

    public String getTmpDir() {
        String workDir = configBean.getString(MbTransformerConfig.TMP_DIR, true);
        if (!isDirectory(workDir)) {
            log.error("Is not directory {}={}", MbTransformerConfig.TMP_DIR.name(), workDir);
            return null;
        }
        return workDir;
    }

    public MbFile getMbFile(String filename) throws FileNotFoundException {
        String workDir = getWorkDir();
        File file = new File(workDir, filename);
        if (!file.isFile()) {
            log.error("{} is not file", file.getPath());
            return null;
        }
        return new MbFile(workDir, filename);
    }

    public boolean isDirectory(String tmpDirPath) {
        File tmpDir = new File(tmpDirPath);
        return tmpDir.exists() && tmpDir.isDirectory();
    }
}
