package ru.flexpay.eirc.mb_transformer.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.charset.Charset;

public abstract class MbParsingConstants {
    public static final String LAST_FILE_STRING_BEGIN = "999999999";
	public static final Charset REGISTRY_FILE_CHARSET = Charset.forName("Cp866");
    public static final String BUILDING_BULK_PREFIX = "К";
    public static final DateTimeFormatter FILE_CREATION_DATE_FORMAT = DateTimeFormat.forPattern("ddMMyy");
    public static final DateTimeFormatter OPERATION_MONTH_DATE_FORMAT = DateTimeFormat.forPattern("MMyy");
    public static final String FIRST_FILE_STRING =
			"                                                                                                    "
			+ "                                                                                                    "
			+ "                                                                                                    ";
	public static final int FIRST_FILE_STRING_SIZE = 300;

    public static final DateTimeFormatter CORRECTIONS_MODIFICATIONS_START_DATE_FORMAT = DateTimeFormat.forPattern("ddMMyy");
    public static final DateTimeFormatter CHARGES_MODIFICATIONS_START_DATE_FORMAT = DateTimeFormat.forPattern("MMyy");
    public static final DateTimeFormatter OPERATION_DATE_FORMAT = DateTimeFormat.forPattern("ddMMyyyy");

    public static final String DELIMITER = "=";

    public static final long FIELDS_LENGTH_SKIP_RECORD = 20;
}
