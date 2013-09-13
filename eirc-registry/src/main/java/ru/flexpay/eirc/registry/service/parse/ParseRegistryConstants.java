package ru.flexpay.eirc.registry.service.parse;

import java.text.SimpleDateFormat;

/**
 * @author Pavel Sknar
 */
public class ParseRegistryConstants {
    /**
     * Symbol used escape special symbols
     */
    public static final char ESCAPE_SYMBOL = '\\';

    /**
     * Symbol used to split containers
     */
    public static final char CONTAINER_DELIMITER = '|';

    /**
     * Symbol used to split fields in containers
     */
    public static final char CONTAINER_DATA_DELIMITER = ':';

    /**
     * Symbol used to split fields in records
     */
    public static final char RECORD_DELIMITER = ';';

    /**
     * Symbol used to split fields in address group
     */
    public static final char ADDRESS_DELIMITER = ',';

    /**
     * Symbol used to split fields in first-middle-last names group
     */
    public static final char FIO_DELIMITER = ',';

    public static final int MAX_CONTAINER_SIZE = 2048;

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyyyyHHmmss");
    public static final int MESSAGE_TYPE_HEADER = 0xC;
    public static final int MESSAGE_TYPE_RECORD = 0x3;
    public static final int MESSAGE_TYPE_FOOTER = 0xB;
}
