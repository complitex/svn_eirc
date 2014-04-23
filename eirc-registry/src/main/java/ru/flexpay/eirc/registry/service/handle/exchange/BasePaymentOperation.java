package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service_provider_account.entity.PaymentAttribute;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.PaymentAttributeBean;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class BasePaymentOperation <T extends PaymentAttribute> extends BaseAccountOperation {

    @Override
    public void process(Registry registry, RegistryRecordData registryRecord, Container container, List<OperationResult> results) throws AbstractException {

        List<String> containerData = splitEscapableData(container.getData());
        if (containerData.size() != 3) {
            throw new ContainerDataException("Failed container format: {0}", container);
        }

        if (Long.parseLong(containerData.get(1)) != registry.getSenderOrganizationId()) {
            throw new ContainerDataException("Failed container data: payment collector differs from sender in payment container {0}", container);
        }

        ServiceProviderAccount serviceProviderAccount = getServiceProviderAccount(registry, registryRecord);

        T data = getBean().getInstance();

        data.setServiceProviderAccount(serviceProviderAccount);
        data.setAmount(registryRecord.getAmount());
        data.setDateFormation(registryRecord.getOperationDate());
        data.setNumberQuittance(containerData.get(2));
        data.setPaymentCollectorId(registry.getSenderOrganizationId());

        getBean().save(data);

        results.add(new OperationResult<>(null, data, getCode()));

    }

    abstract protected PaymentAttributeBean<T> getBean();
}

