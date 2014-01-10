package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.ImmutableSet;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Set;

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

    public static final Set<Character> DELIMITERS = ImmutableSet.of(
            CONTAINER_DELIMITER,
            CONTAINER_DATA_DELIMITER,
            RECORD_DELIMITER,
            ADDRESS_DELIMITER
    );

    public static final DateTimeFormatter HEADER_DATE_FORMAT = DateTimeFormat.forPattern("ddMMyyyyHHmmss");
    public static final DateTimeFormatter RECORD_DATE_FORMAT = DateTimeFormat.forPattern("ddMMyyyyHHmmss");
    public static final int MESSAGE_TYPE_HEADER = 0xC;
    public static final int MESSAGE_TYPE_RECORD = 0x3;
    public static final int MESSAGE_TYPE_FOOTER = 0xB;
}
