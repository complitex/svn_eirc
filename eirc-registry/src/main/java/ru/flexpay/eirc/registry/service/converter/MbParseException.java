package ru.flexpay.eirc.registry.service.converter;

import org.complitex.dictionary.service.exception.AbstractException;

/**
 * @author Pavel Sknar
 */
public class MbParseException extends AbstractException {
    public MbParseException(boolean initial, Throwable cause, String pattern, Object... arguments) {
        super(initial, cause, pattern, arguments);
    }

    public MbParseException(Throwable cause, String pattern, Object... arguments) {
        super(cause, pattern, arguments);
    }

    public MbParseException(String pattern, Object... arguments) {
        super(pattern, arguments);
    }
}
