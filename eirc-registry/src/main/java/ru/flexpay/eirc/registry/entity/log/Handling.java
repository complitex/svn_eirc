package ru.flexpay.eirc.registry.entity.log;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

/**
 * @author Pavel Sknar
 */
@BaseName("ru.flexpay.eirc.registry.entity.log.handling")
@LocaleData(defaultCharset = "UTF-8", value = {@Locale("ru"), @Locale("en")})
public enum Handling {
    STARTING_HANDLE_REGISTRIES,
    REGISTRY_FAILED_STATUS,
    REGISTRY_STATUS_INNER_ERROR,
    NOT_FOUND_HANDLING_REGISTRY_RECORDS,
    REGISTRY_FAILED_HANDLED,
    REGISTRY_FINISH_HANDLE,
    HANDLED_BULK_RECORDS,
    HANDLING_CANCELED,

}
