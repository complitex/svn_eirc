package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import ru.flexpay.eirc.registry.util.StringUtil;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class RegistryUtil {
    /**
     * Parse registry FIO group
     *
     * @param fioStr Group string
     * @return Last-First-Middle names list
     * @throws RegistryFormatException if fioStr is invalid
     */
    public static java.util.List<String> parseFIO(String fioStr) throws RegistryFormatException {

        if (StringUtils.isBlank(fioStr)) {
            return Lists.newArrayList("", "", "");
        }

        List<String> fields = StringUtil.splitEscapable(
                fioStr, ParseRegistryConstants.FIO_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
        if (fields.size() != 3) {
            throw new RegistryFormatException(
                    String.format("FIO group '%s' has invalid number of fields %d",
                            fioStr, fields.size()));
        }

        return fields;
    }
}
