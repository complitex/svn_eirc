package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.ContainerType;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class CloseAccountOperation extends GeneralAccountOperation {

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    @EJB
    private EircAccountBean eircAccountBean;

    @Override
    public Long getCode() {
        return ContainerType.CLOSE_ACCOUNT.getId();
    }

    @Override
    public void process(Registry registry, RegistryRecordData registryRecord, Container container, List<OperationResult> results) throws AbstractException {
        ServiceProviderAccount serviceProviderAccount = getServiceProviderAccount(registry, registryRecord);

        List<ServiceProviderAccount> serviceProviderAccounts =
                serviceProviderAccountBean.getServiceProviderAccounts(
                        new FilterWrapper<>(new ServiceProviderAccount(serviceProviderAccount.getEircAccount())));

        if (serviceProviderAccounts.size() == 0 ||
                (serviceProviderAccounts.size() == 1 &&
                        serviceProviderAccounts.get(0).getPkId().equals(serviceProviderAccount.getPkId()))) {
            // if service provider accounts list is empty then close EIRC account
            eircAccountBean.archive(serviceProviderAccount.getEircAccount());
        }

        serviceProviderAccountBean.archive(serviceProviderAccount);
    }
}
