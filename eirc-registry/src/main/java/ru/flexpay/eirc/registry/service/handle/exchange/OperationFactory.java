package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.address.strategy.city.CityStrategy;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;
import ru.flexpay.eirc.service_provider_account.strategy.ServiceProviderAccountStrategy;

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
    private ServiceProviderAccountStrategy serviceProviderAccountStrategy;

    @EJB
    private CityStrategy cityStrategy;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private LocaleBean localeBean;

    private Locale systemLocale;

    public Operation getOperation(Registry registry, RegistryRecord registryRecord, Container container) throws AbstractException {
        switch (container.getType()) {
            case OPEN_ACCOUNT:
                OpenAccountOperation operation = new OpenAccountOperation(registry, registryRecord, container);
                operation.setCityStrategy(cityStrategy);
                operation.setEircAccountBean(eircAccountBean);
                operation.setServiceBean(serviceBean);
                operation.setServiceProviderAccountBean(serviceProviderAccountBean);
                return operation;
            case SET_NUMBER_ON_HABITANTS:
                SetNumberOfHabitantsOperation setNumberOfHabitantsOperation =
                        new SetNumberOfHabitantsOperation(registry, registryRecord, container);
                setNumberOfHabitantsOperation.setServiceProviderAccountBean(serviceProviderAccountBean);
                setNumberOfHabitantsOperation.setLocaleBean(localeBean);
                setNumberOfHabitantsOperation.setServiceProviderAccountStrategy(serviceProviderAccountStrategy);
                return setNumberOfHabitantsOperation;
            case SET_TOTAL_SQUARE:
                SetTotalSquareOperation setTotalSquareOperation =
                        new SetTotalSquareOperation(registry, registryRecord, container);
                setTotalSquareOperation.setServiceProviderAccountBean(serviceProviderAccountBean);
                setTotalSquareOperation.setLocaleBean(localeBean);
                setTotalSquareOperation.setServiceProviderAccountStrategy(serviceProviderAccountStrategy);
                return setTotalSquareOperation;
            case SET_LIVE_SQUARE:
                SetLiveSquareOperation setLiveSquareOperation =
                        new SetLiveSquareOperation(registry, registryRecord, container);
                setLiveSquareOperation.setServiceProviderAccountBean(serviceProviderAccountBean);
                setLiveSquareOperation.setLocaleBean(localeBean);
                setLiveSquareOperation.setServiceProviderAccountStrategy(serviceProviderAccountStrategy);
                return setLiveSquareOperation;
            case SET_WARM_SQUARE:
                SetHeatedSquareOperation setHeatedSquareOperation =
                        new SetHeatedSquareOperation(registry, registryRecord, container);
                setHeatedSquareOperation.setServiceProviderAccountBean(serviceProviderAccountBean);
                setHeatedSquareOperation.setLocaleBean(localeBean);
                setHeatedSquareOperation.setServiceProviderAccountStrategy(serviceProviderAccountStrategy);
                return setHeatedSquareOperation;

        }

        throw new ContainerDataException("Unknown container type: {0}", container);
    }
}
