package ru.flexpay.eirc.service_provider_account.service;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.SqlSessionFactoryBean;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.SequenceBean;
import org.complitex.dictionary.util.DateUtil;
import ru.flexpay.eirc.eirc_account.service.EircAccountBean;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceProviderAccountBean extends AbstractBean {

    private static final String NS = ServiceProviderAccountBean.class.getPackage().getName() + ".ServiceProviderAccountBean";
    public static final String ENTITY_TABLE = "service_provider_account";

    public static final String FILTER_MAPPING_ATTRIBUTE_NAME = "serviceProviderAccount";

    @EJB
    private SequenceBean sequenceBean;

    @EJB
    private LocaleBean localeBean;

    @Transactional
    public void archive(ServiceProviderAccount object) {
        if (object.getEndDate() == null) {
            object.setEndDate(DateUtil.getCurrentDate());
        }
        sqlSession().update(NS + ".updateServiceProviderAccountEndDate", object);
    }

    public ServiceProviderAccount getServiceProviderAccount(long id) {
        List<ServiceProviderAccount> resultOrderByDescData = sqlSession().selectList(NS + ".selectServiceProviderAccount", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<ServiceProviderAccount> getServiceProviderAccounts(FilterWrapper<ServiceProviderAccount> filter) {
        addFilterMappingObject(filter);
        if (filter != null && StringUtils.equals(filter.getSortProperty(), "id")) {
            filter.setSortProperty("spa_object_id");
        }
        return sqlSession().selectList(NS + ".selectServiceProviderAccounts", filter);
    }

    public int count(FilterWrapper<ServiceProviderAccount> filter) {
        addFilterMappingObject(filter);
        return sqlSession().selectOne(NS + ".countServiceProviderAccounts", filter);
    }

    @Transactional
    public void save(ServiceProviderAccount serviceProviderAccount) {
        if (serviceProviderAccount.getId() == null) {
            saveNew(serviceProviderAccount);
        } else {
            update(serviceProviderAccount);
        }
    }

    private void saveNew(ServiceProviderAccount serviceProviderAccount) {
        serviceProviderAccount.setId(sequenceBean.nextId(ENTITY_TABLE));
        sqlSession().insert(NS + ".insertServiceProviderAccount", serviceProviderAccount);
    }

    private void update(ServiceProviderAccount serviceProviderAccount) {
        ServiceProviderAccount oldObject = getServiceProviderAccountByPkId(serviceProviderAccount.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, serviceProviderAccount)) {
            return;
        }
        archive(oldObject);
        serviceProviderAccount.setBeginDate(oldObject.getEndDate());
        sqlSession().insert(NS + ".insertServiceProviderAccount", serviceProviderAccount);
    }

    public ServiceProviderAccount getServiceProviderAccountByPkId(long pkId) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("pkId", pkId);
        params.put("locale", localeBean.convert(localeBean.getSystemLocale()).getId());
        return sqlSession().selectOne(NS + ".selectServiceProviderAccountByPkId", pkId);
    }

    private void addFilterMappingObject(FilterWrapper<ServiceProviderAccount> filter) {
        if (filter != null) {
            if (filter.getObject() != null) {
                ServiceBean.addFilterMappingObject(filter, filter.getObject().getService());
                EircAccountBean.addFilterMappingObject(filter, filter.getObject().getEircAccount());
            }
            addFilterMappingObject(filter, filter.getObject());
        }
    }

    public void addFilterMappingObject(FilterWrapper<?> filter,
                                              ServiceProviderAccount serviceProviderAccount) {
        if (filter != null) {
            filter.getMap().put(FILTER_MAPPING_ATTRIBUTE_NAME, serviceProviderAccount);
        }
    }

    @Override
    public void setSqlSessionFactoryBean(SqlSessionFactoryBean sqlSessionFactoryBean) {
        super.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        sequenceBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        localeBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
    }
}
