package ru.flexpay.eirc.registry.entity.log;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

/**
 * @author Pavel Sknar
 */
@BaseName("ru.flexpay.eirc.registry.entity.log.general_processing")
@LocaleData(defaultCharset = "UTF-8", value = {@Locale("ru"), @Locale("en")})
public enum GeneralProcessing {
    EIRC_ORGANIZATION_ID_NOT_DEFINED,
    REGISTRY_NOT_FOUND,

}
