package ru.flexpay.eirc.service_provider_account.service;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.SqlSessionFactoryBean;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.SequenceBean;
import org.complitex.dictionary.util.DateUtil;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.service_provider_account.entity.ServiceNotAllowableException;
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

    @EJB
    private EircOrganizationStrategy eircOrganizationStrategy;

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
        ServiceProviderAccountUtil.addFilterMappingObject(filter);
        if (filter != null && StringUtils.equals(filter.getSortProperty(), "id")) {
            filter.setSortProperty("spa_object_id");
        }
        return sqlSession().selectList(NS + ".selectServiceProviderAccounts", filter);
    }

    public int count(FilterWrapper<ServiceProviderAccount> filter) {
        ServiceProviderAccountUtil.addFilterMappingObject(filter);
        return sqlSession().selectOne(NS + ".countServiceProviderAccounts", filter);
    }

    public void save(ServiceProviderAccount serviceProviderAccount) throws ServiceNotAllowableException {
        validate(serviceProviderAccount);

        if (serviceProviderAccount.getId() == null) {
            saveNew(serviceProviderAccount);
        } else {
            update(serviceProviderAccount);
        }
    }

    public void validate(ServiceProviderAccount serviceProviderAccount) throws ServiceNotAllowableException {
        if (serviceProviderAccount.getService() != null && serviceProviderAccount.getService().getId() != null) {
            // check allowable services
            Organization organization = eircOrganizationStrategy.findById(serviceProviderAccount.getOrganizationId(), true);
            List<Attribute> allowableServices = organization.getAttributes(EircOrganizationStrategy.SERVICE);
            boolean check = false;
            for (Attribute allowableService : allowableServices) {
                 if (serviceProviderAccount.getService().getId().equals(allowableService.getValueId())) {
                     check = true;
                     break;
                }
            }
            if (!check) {
                throw new ServiceNotAllowableException("Service {0} do not allowable for organization {1}",
                        serviceProviderAccount.getService(), serviceProviderAccount.getId());
            }
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

    @Override
    public void setSqlSessionFactoryBean(SqlSessionFactoryBean sqlSessionFactoryBean) {
        super.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        sequenceBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        localeBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        eircOrganizationStrategy.setSqlSessionFactoryBean(sqlSessionFactoryBean);
    }
}
