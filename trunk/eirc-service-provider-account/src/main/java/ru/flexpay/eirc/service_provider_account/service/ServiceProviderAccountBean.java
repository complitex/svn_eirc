package ru.flexpay.eirc.service_provider_account.service;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceProviderAccountBean extends AbstractBean {

    private static final String NS = ServiceProviderAccountBean.class.getPackage().getName() + ".ServiceProviderAccountBean";
    public static final String ENTITY_TABLE = "service_provider_account";

    @EJB
    private SequenceBean sequenceBean;

    @Transactional
    public void archive(ServiceProviderAccount object) {
        if (object.getEndDate() == null) {
            object.setEndDate(new Date());
        }
        sqlSession().update("updateEndDate", object);
    }

    public ServiceProviderAccount getServiceProviderAccount(long id) {
        List<ServiceProviderAccount> resultOrderByDescData = sqlSession().selectList(NS + ".selectServiceProviderAccount", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<ServiceProviderAccount> getServiceProviderAccounts(FilterWrapper<ServiceProviderAccount> filter) {
        return sqlSession().selectList(NS + ".selectServiceProviderAccounts", filter);
    }

    public int count(FilterWrapper<ServiceProviderAccount> filter) {
        return sqlSession().selectOne(NS + ".countServiceProviderAccounts", filter);
    }

    @Transactional
    public void save(ServiceProviderAccount serviceProviderAccount) {
        serviceProviderAccount.setId(sequenceBean.nextId(ENTITY_TABLE));
        sqlSession().insert(NS + ".insert", serviceProviderAccount);
    }

    @Transactional
    public void update(ServiceProviderAccount serviceProviderAccount) {
        ServiceProviderAccount oldObject = getEricAccountByPkId(serviceProviderAccount.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, serviceProviderAccount)) {
            return;
        }
        archive(oldObject);
        serviceProviderAccount.setBeginDate(oldObject.getEndDate());
        sqlSession().insert(NS + ".insert", serviceProviderAccount);
    }

    public ServiceProviderAccount getEricAccountByPkId(long pkId) {
        return sqlSession().selectOne(NS + ".selectServiceProviderAccountByPkId", pkId);
    }

}
