package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.service.exception.AbstractException;

/**
 * @author Pavel Sknar
 */
public class ContainerDataException extends AbstractException {
    public ContainerDataException(boolean initial, Throwable cause, String pattern, Object... arguments) {
        super(initial, cause, pattern, arguments);
    }

    public ContainerDataException(Throwable cause, String pattern, Object... arguments) {
        super(cause, pattern, arguments);
    }

    public ContainerDataException(String pattern, Object... arguments) {
        super(pattern, arguments);
    }
}
