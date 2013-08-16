package ru.flexpay.eirc.service.service;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceBean extends AbstractBean {

    private static final String NS = ServiceBean.class.getPackage().getName() + ".ServiceBean";

    @EJB
    private SequenceBean sequenceBean;

    @Transactional
    public void delete(Service service) {
        sqlSession().delete("delete", service);
    }

    public Service getService(long id) {
        List<Service> resultOrderByDescData = sqlSession().selectList(NS + ".selectService", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<Service> getServices(FilterWrapper<Service> filter) {
        return sqlSession().selectList(NS + ".selectServices", filter);
    }

    public int count(FilterWrapper<Service> filter) {
        return sqlSession().selectOne(NS + ".countServices", filter);
    }

    @Transactional
    public void save(Service service) {
        sqlSession().insert(NS + ".insert", service);
    }

    @Transactional
    public void update(Service service) {
        Service oldObject = getService(service.getId());
        if (EqualsBuilder.reflectionEquals(oldObject, service)) {
            return;
        }
        sqlSession().update(NS + ".update", service);
    }

}
