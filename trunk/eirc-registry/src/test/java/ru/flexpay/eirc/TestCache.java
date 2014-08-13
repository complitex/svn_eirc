package ru.flexpay.eirc;

import org.apache.commons.lang.time.StopWatch;
import org.complitex.address.entity.AddressEntity;
import org.complitex.dictionary.EjbTestBeanLocator;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.util.CloneUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.AbstractMessenger;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.link.RegistryLinker;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceNotAllowableException;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Pavel Sknar
 */
public class TestCache {

    private static final Logger log = LoggerFactory.getLogger(TestTransaction.class);

    @Test
    public void testChangeService() throws NamingException, ServiceNotAllowableException {

        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {
            final Context context = container.getContext();

            ServiceBean serviceBean = EjbTestBeanLocator.getBean(context, "ServiceBean");
            ServiceProviderAccountBean serviceProviderAccountBean = EjbTestBeanLocator.getBean(context, "ServiceProviderAccountBean");
            EircAccountBean eircAccountBean = EjbTestBeanLocator.getBean(context, "EircAccountBean");
            EircOrganizationStrategy eircOrganizationStrategy = EjbTestBeanLocator.getBean(context, "OrganizationStrategy");

            assertNotNull(serviceBean);
            assertNotNull(serviceProviderAccountBean);
            assertNotNull(eircAccountBean);
            assertNotNull(eircOrganizationStrategy);

            Service service;
            final Long organizationId = 1L;

            Organization organization = eircOrganizationStrategy.findById(organizationId, true);
            List<Attribute> services = organization.getAttributes(EircOrganizationStrategy.SERVICE);
            if (services.get(0).getValueId() == null) {

                service = new Service();
                service.setNameRu("TestService1");
                service.setCode("1");
                serviceBean.save(service);

                Organization oldOrganization = CloneUtil.cloneObject(organization);
                Attribute serviceAttribute = services.get(0);
                serviceAttribute.setValueId(service.getId());
                serviceAttribute.setAttributeTypeId(EircOrganizationStrategy.SERVICE);
                serviceAttribute.setObjectId(organizationId);

                eircOrganizationStrategy.update(oldOrganization, organization, new Date());
            } else {
                Attribute serviceAttribute = services.get(0);
                service = serviceBean.getService(serviceAttribute.getValueId());
                assertNotNull(service);
            }

            EircAccount eircAccount = new EircAccount("TestEircAccount1");
            eircAccount.setAddress(new Address(1L, AddressEntity.APARTMENT));
            eircAccountBean.save(eircAccount);

            ServiceProviderAccount serviceProviderAccount = new ServiceProviderAccount(eircAccount, service);
            serviceProviderAccount.setOrganizationId(organizationId);
            serviceProviderAccount.setAccountNumber("TestSPAAccount1");
            serviceProviderAccountBean.save(serviceProviderAccount);

            serviceProviderAccount = serviceProviderAccountBean.getServiceProviderAccount(serviceProviderAccount.getId());

            service.setCode(String.valueOf(parseInt(service.getCode()) + 1));
            serviceBean.save(service);

            assertNotEquals(serviceProviderAccount, serviceProviderAccountBean.getServiceProviderAccount(serviceProviderAccount.getId()));
        }

    }

    @Test
    public void testDeleteService() {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {
            final Context context = container.getContext();

            ServiceBean serviceBean = EjbTestBeanLocator.getBean(context, "ServiceBean");

            Service service = new Service();
            service.setNameRu("TestService2");
            service.setCode("2");
            serviceBean.save(service);

            serviceBean.save(service);

            service.setCode("3");
            serviceBean.save(service);

            serviceBean.delete(service);
        }
    }

    @Test
    public void testLinker() throws InterruptedException {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {
            final Context context = container.getContext();

            RegistryLinker registryLinker = EjbTestBeanLocator.getBean(context, "RegistryLinker");

            AbstractFinishCallback finishCallback = new AbstractFinishCallback() {
                private AtomicInteger counter = new AtomicInteger(0);

                @Override
                protected AtomicInteger getCounter() {
                    return counter;
                }

                @Override
                public void setProcessId(Long processId) {

                }
            };

            final Locale locale = new Locale("ru");
            StopWatch watch = new StopWatch();
            watch.start();
            registryLinker.link(9L, new AbstractMessenger() {
                private final String RESOURCE_BUNDLE = RegistryMessenger.class.getName();

                Queue<IMessage> messages = new Queue<IMessage>() {
                    @Override
                    public boolean add(IMessage iMessage) {
                        System.out.println(iMessage.getLocalizedString(locale));
                        return true;
                    }

                    @Override
                    public boolean offer(IMessage iMessage) {
                        return false;
                    }

                    @Override
                    public IMessage remove() {
                        return null;
                    }

                    @Override
                    public IMessage poll() {
                        return null;
                    }

                    @Override
                    public IMessage element() {
                        return null;
                    }

                    @Override
                    public IMessage peek() {
                        return null;
                    }

                    @Override
                    public int size() {
                        return 0;
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public boolean contains(Object o) {
                        return false;
                    }

                    @Override
                    public Iterator<IMessage> iterator() {
                        return null;
                    }

                    @Override
                    public Object[] toArray() {
                        return new Object[0];
                    }

                    @Override
                    public <T> T[] toArray(T[] a) {
                        return null;
                    }

                    @Override
                    public boolean remove(Object o) {
                        return false;
                    }

                    @Override
                    public boolean containsAll(Collection<?> c) {
                        return false;
                    }

                    @Override
                    public boolean addAll(Collection<? extends IMessage> c) {
                        return false;
                    }

                    @Override
                    public boolean removeAll(Collection<?> c) {
                        return false;
                    }

                    @Override
                    public boolean retainAll(Collection<?> c) {
                        return false;
                    }

                    @Override
                    public void clear() {

                    }
                };

                @Override
                public String getResourceBundle() {
                    return RESOURCE_BUNDLE;
                }

                @Override
                public Queue<IMessage> getIMessages() {
                    return messages;
                }

            }, finishCallback);
            while (!finishCallback.isCompleted()) {
                Thread.sleep(1000L);
            }
            watch.stop();

            System.out.println("Watch time: " + watch.toString());
        }
    }
}
