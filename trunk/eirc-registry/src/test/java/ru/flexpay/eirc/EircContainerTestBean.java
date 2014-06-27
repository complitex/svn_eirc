package ru.flexpay.eirc;

import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.TestBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Pavel Sknar
 */
@Stateless
public class EircContainerTestBean extends AbstractBean {

    @EJB
    private TestBean testBean;

    @Transactional
    public void testTransactional() {
        testBean.testSelectSimple("");
    }

    @Transactional
    public void testTransactionalWithSleep(long time) {
        testBean.testSelectSimpleWithSleep("", time);
        //testBean.testSelectSimple("");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            //
        }
    }

}
