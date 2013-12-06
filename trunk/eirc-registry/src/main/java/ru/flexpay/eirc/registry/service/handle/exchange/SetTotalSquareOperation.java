package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

/**
 * @author Pavel Sknar
 */
public class SetTotalSquareOperation extends ServiceProviderAccountAttrOperation {

    /**
     * Parse data and set operation id. Executing {@link ru.flexpay.eirc.registry.service.handle.exchange.Operation#prepareData}
     *
     * @param container Container
     * @throws org.complitex.dictionary.service.exception.AbstractException
     *
     */
    public SetTotalSquareOperation(Registry registry, RegistryRecord registryRecord, Container container) throws AbstractException {
        super(registry, registryRecord, container);
    }

    @Override
    protected Long getAttributeId() {
        return ServiceProviderAccountStrategy.TOTAL_SQUARE;
    }
}
