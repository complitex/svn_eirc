package ru.flexpay.eirc.service_provider_account.entity;

import org.complitex.dictionary.service.exception.AbstractException;

/**
 * @author Pavel Sknar
 */
public class ServiceNotAllowableException extends AbstractException {
    public ServiceNotAllowableException(boolean initial, Throwable cause, String pattern, Object... arguments) {
        super(initial, cause, pattern, arguments);
    }

    public ServiceNotAllowableException(Throwable cause, String pattern, Object... arguments) {
        super(cause, pattern, arguments);
    }

    public ServiceNotAllowableException(String pattern, Object... arguments) {
        super(pattern, arguments);
    }
}
