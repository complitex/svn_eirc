package ru.flexpay.eirc.registry.entity.log;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

/**
 * @author Pavel Sknar
 */
@BaseName("ru.flexpay.eirc.registry.entity.log.linking")
@LocaleData(defaultCharset = "UTF-8", value = {@Locale("ru"), @Locale("en")})
public enum Linking {
    STARTING_LINK_REGISTRIES,
    REGISTRY_FAILED_STATUS,
    REGISTRY_STATUS_INNER_ERROR,
    NOT_FOUND_LINKING_REGISTRY_RECORDS,
    REGISTRY_FAILED_LINKED,
    REGISTRY_FINISH_LINK,
    LINKED_BULK_RECORDS,
    LINKING_CANCELED,

}
