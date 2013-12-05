package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.address.strategy.city.CityStrategy;
import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * @author Pavel Sknar
 */
@Singleton
public class OperationFactory {

    @EJB
    private EircAccountBean eircAccountBean;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private CityStrategy cityStrategy;

    @EJB
    private ServiceBean serviceBean;

    public OperationFactory() {
    }

    public Operation getOperation(Registry registry, RegistryRecord registryRecord, Container container) throws AbstractException {
        switch (container.getType()) {
            case OPEN_ACCOUNT:
                OpenAccountOperation operation = new OpenAccountOperation(registry, registryRecord, container);
                operation.setCityStrategy(cityStrategy);
                operation.setEircAccountBean(eircAccountBean);
                operation.setServiceBean(serviceBean);
                operation.setServiceProviderAccountBean(serviceProviderAccountBean);
                return operation;
        }

        throw new ContainerDataException("Unknown container type: {0}", container);
    }
}
