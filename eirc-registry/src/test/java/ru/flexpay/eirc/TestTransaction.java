package ru.flexpay.eirc;

import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.complitex.dictionary.EjbTestBeanLocator;
import org.complitex.dictionary.service.TestBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.service.AbstractJob;
import ru.flexpay.eirc.registry.service.JobProcessor;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Sknar
 */
public class TestTransaction {

    private static final Logger log = LoggerFactory.getLogger(TestTransaction.class);

    @Test
    public void testTransactional() throws NamingException {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {

            final Context context = container.getContext();
            JobProcessor jobProcessor = EjbTestBeanLocator.getBean(context, "JobProcessor");

            int n = 2;

            final Semaphore finished = new Semaphore(-1 * (n - 1), false);

            final List<ExceptionInfo> exceptionInfos = Collections.synchronizedList(Lists.<ExceptionInfo>newArrayList());

            for (int i = 0; i < n; i++) {
                final int j = 10000 * i + 1;
                final int m = 10000 * (i + 1);
                jobProcessor.processJob(new AbstractJob<Object>() {
                    @Override
                    public Object execute() throws ExecuteException {

                        try {
                            for (long idx = j; idx < m; idx++) {
                                EircOrganizationStrategy organizationStrategy = EjbTestBeanLocator.getBean(context, EircOrganizationStrategy.BEAN_NAME);
                                assertNotNull("Can not find EircOrganizationStrategy", organizationStrategy);
                                try {
                                    organizationStrategy.findById(idx, false);
                                } catch (Exception e) {
                                    exceptionInfos.add(new ExceptionInfo(new Date(), idx));
                                    return null;
                                }
                            }

                        } finally {
                            finished.release();
                        }
                        return null;
                    }
                });
            }
            try {
                finished.acquire();
            } catch (InterruptedException e) {
                //
            }
            assertTrue("Exceptions did not happen", exceptionInfos.size() > 0);
            log.info("test ended: {}", exceptionInfos);
        }
    }

    @Test
    public void testTransactional2() throws NamingException {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {
            final Context context = container.getContext();
            JobProcessor jobProcessor = EjbTestBeanLocator.getBean(context, "JobProcessor");

            int n = 2;

            final Semaphore finished = new Semaphore(-1 * (n - 1), false);

            final List<ExceptionInfo> exceptionInfos = Collections.synchronizedList(Lists.<ExceptionInfo>newArrayList());

            final Random random = new Random(System.currentTimeMillis());

            for (int i = 0; i < n; i++) {
                final int j = 1000 * i + 1;
                final int m = 1000 * (i + 1);
                jobProcessor.processJob(new AbstractJob<Object>() {
                    @Override
                    public Object execute() throws ExecuteException {

                        try {
                            for (long idx = j; idx < m; idx++) {
                                EircContainerTestBean containerTestBean = EjbTestBeanLocator.getBean(context, "EircContainerTestBean");
                                assertNotNull("Can not find ContainerTestBean", containerTestBean);

                                long sleepTime = 0;
                                //long sleepTime = Math.abs(random.nextLong()%100);
                                try {
                                    //containerTestBean.testTransactionalWithSleep(sleepTime);
                                    containerTestBean.testTransactional();
                                } catch (Exception e) {
                                    log.info("exception {} - idx: {}, sleep: {}", Thread.currentThread().getId(), idx, sleepTime);
                                    exceptionInfos.add(new ExceptionInfo(new Date(), idx));
                                    return null;
                                }
                                if (exceptionInfos.size() > 0) {
                                    log.info("interrupted {} - idx: {}, sleep: {}", Thread.currentThread().getId(), idx, sleepTime);
                                    return null;
                                }
                            }

                        } finally {
                            finished.release();
                        }
                        return null;
                    }
                });
            }
            try {
                finished.acquire();
            } catch (InterruptedException e) {
                //
            }
            assertTrue("Exceptions did not happen", exceptionInfos.size() > 0);
            log.info("test ended: {}", exceptionInfos);
        }
    }

    @Test
    public void testTransactional3() throws NamingException {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {

            final Context context = container.getContext();
            int n = 2;

            final Semaphore finished = new Semaphore(-1 * (n - 1), false);

            final List<ExceptionInfo> exceptionInfos = Collections.synchronizedList(Lists.<ExceptionInfo>newArrayList());
            final AtomicBoolean canNotFindTestBean = new AtomicBoolean(false);

            for (int i = 0; i < n; i++) {
                final int j = 1000 * i + 1;
                final int m = 1000 * (i + 1);
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            for (long idx = j; idx < m; idx++) {
                                EircContainerTestBean containerTestBean = EjbTestBeanLocator.getBean(context, "EircContainerTestBean");
                                if (containerTestBean == null) {
                                    canNotFindTestBean.set(true);
                                    return;
                                }
                                try {
                                    containerTestBean.testTransactional();
                                } catch (Exception e) {
                                    exceptionInfos.add(new ExceptionInfo(new Date(), idx));
                                    return;
                                }
                            }

                        } finally {
                            finished.release();
                        }
                    }
                }).run();
            }
            try {
                finished.acquire();
            } catch (InterruptedException e) {
                //
            }
            log.info("test ended: {}", exceptionInfos);
            assertNotNull("Can not find TestBean", canNotFindTestBean.get());
            assertTrue("Exceptions happen", exceptionInfos.size() == 0);
        }
    }

    @Test
    public void testTransactional4() throws NamingException {
        try (final EJBContainer container = EjbTestBeanLocator.createEJBContainer()) {

            final Context context = container.getContext();
            JobProcessor jobProcessor = EjbTestBeanLocator.getBean(context, "JobProcessor");
            int n = 2;

            final Semaphore finished = new Semaphore(-1 * (n - 1), false);

            final List<ExceptionInfo> exceptionInfos = Collections.synchronizedList(Lists.<ExceptionInfo>newArrayList());
            final AtomicBoolean canNotFindTestBean = new AtomicBoolean(false);


            for (int i = 0; i < n; i++) {
                final int j = 10000 * i + 1;
                final int m = 10000 * (i + 1);
                jobProcessor.processJob(new AbstractJob<Object>() {
                    @Override
                    public Object execute() throws ExecuteException {

                        try {
                            for (long idx = j; idx < m; idx++) {
                                TestBean testBean = EjbTestBeanLocator.getBean(context, "TestBean");
                                if (testBean == null) {
                                    canNotFindTestBean.set(true);
                                    return null;
                                }
                                try {
                                    testBean.testSelectTransactional("");
                                } catch (Exception e) {
                                    exceptionInfos.add(new ExceptionInfo(new Date(), idx));
                                    return null;
                                }
                            }

                        } finally {
                            finished.release();
                        }
                        return null;
                    }
                });
            }
            try {
                finished.acquire();
            } catch (InterruptedException e) {
                //
            }
            log.info("test ended: {}", exceptionInfos);
            assertNotNull("Can not find TestBean", canNotFindTestBean.get());
            assertTrue("Exceptions happen", exceptionInfos.size() == 0);
        }
    }

    private class ExceptionInfo {
        private Date date;
        private long idx;

        private ExceptionInfo(Date date, long idx) {
            this.date = date;
            this.idx = idx;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("date", date)
                    .append("idx", idx)
                    .toString();
        }
    }
}
