package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.service.exception.AbstractException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service_provider_account.entity.FinancialAttribute;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.FinancialAttributeBean;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class BaseFinancialOperation<T extends FinancialAttribute> extends BaseAccountOperation {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("ddMMyyyy");

    @Override
    public void process(Registry registry, RegistryRecordData registryRecord, Container container, List<OperationResult> results) throws AbstractException {

        List<String> containerData = splitEscapableData(container.getData());
        if (containerData.size() != 3) {
            throw new ContainerDataException("Failed container format: {0}", container);
        }

        ServiceProviderAccount serviceProviderAccount = getServiceProviderAccount(registry, registryRecord);

        T data = getBean().getInstance();
        try {
            data.setServiceProviderAccount(serviceProviderAccount);
            data.setAmount(new BigDecimal(containerData.get(1)));
            data.setDateFormation(DATE_TIME_FORMATTER.parseDateTime(containerData.get(2)).toDate());
        } catch (Exception e) {
            throw new ContainerDataException("Cannot parse date: {0}" + containerData.get(1));
        }

        getBean().save(data);

        results.add(new OperationResult<>(null, data, getCode()));

    }

    abstract protected FinancialAttributeBean<T> getBean();
}
