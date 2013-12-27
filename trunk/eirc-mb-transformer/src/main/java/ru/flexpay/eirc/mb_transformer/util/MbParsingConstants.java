package ru.flexpay.eirc.mb_transformer.util;

import java.text.SimpleDateFormat;

public abstract class MbParsingConstants {
    public static final String LAST_FILE_STRING_BEGIN = "999999999";
	public static final String REGISTRY_FILE_ENCODING = "Cp866";
    public static final String BUILDING_BULK_PREFIX = "К";
    public static final SimpleDateFormat FILE_CREATION_DATE_FORMAT = new SimpleDateFormat("ddMMyy");
    public static final SimpleDateFormat OPERATION_MONTH_DATE_FORMAT = new SimpleDateFormat("MMyy");
    public static final String FIRST_FILE_STRING =
			"                                                                                                    "
			+ "                                                                                                    "
			+ "                                                                                                    ";
	public static final int FIRST_FILE_STRING_SIZE = 300;

    public static final SimpleDateFormat CORRECTIONS_MODIFICATIONS_START_DATE_FORMAT = new SimpleDateFormat("ddMMyy");
    public static final SimpleDateFormat CHARGES_MODIFICATIONS_START_DATE_FORMAT = new SimpleDateFormat("MMyy");
    public static final SimpleDateFormat OPERATION_DATE_FORMAT = new SimpleDateFormat("ddMMyyyy");

    public static final String DELIMITER = "=";

    public static final long FIELDS_LENGTH_SKIP_RECORD = 20;
}
