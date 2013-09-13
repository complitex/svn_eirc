package ru.flexpay.eirc.registry.util;

import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;

import java.io.*;

/**
 * @author Pavel Sknar
 */
public class FileUtil {
    private static boolean isRegistry(File file, Logger log) throws IOException {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();

            return (line.length() > 0 && line.codePointAt(0) == ParseRegistryConstants.MESSAGE_TYPE_HEADER);

        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
