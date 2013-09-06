package ru.flexpay.eirc.registry.service.parse;

import org.complitex.dictionary.mybatis.IFixedIdType;
import org.complitex.dictionary.service.exception.AbstractException;

/**
 * @author Pavel Sknar
 */
public class TransitionNotAllowed extends AbstractException {
    private IFixedIdType type;

    public TransitionNotAllowed(String s) {
        super(s);
    }

    public TransitionNotAllowed(String s, IFixedIdType type) {
        super(s);
        this.type = type;
    }

    public IFixedIdType getType() {
        return type;
    }

    public void setType(IFixedIdType type) {
        this.type = type;
    }
}
