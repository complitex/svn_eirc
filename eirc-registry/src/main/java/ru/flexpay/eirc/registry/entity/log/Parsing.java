package ru.flexpay.eirc.registry.entity.log;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

/**
 * @author Pavel Sknar
 */
@BaseName("ru.flexpay.eirc.registry.entity.log.parsing")
@LocaleData(defaultCharset = "UTF-8", value = {@Locale("ru"), @Locale("en")})
public enum Parsing {
    INNER_ERROR,
    INNER_ERROR_IN_REGISTRY,
    STARTING_UPLOAD_REGISTRIES,
    FILES_NOT_FOUND,
    STARTING_UPLOAD_REGISTRY,
    REGISTRY_FINISH_UPLOAD
}
