package ru.flexpay.eirc.registry.service;

import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryBean extends AbstractBean {
    private static final String NS = RegistryBean.class.getName();

    @EJB
    private RegistryRecordBean registryRecordBean;

    public static final String CREATION_DATE_RANGE = "creationDateRange";
    public static final String LOAD_DATE_RANGE = "loadDateRange";

    public List<Registry> getRegistries(FilterWrapper<Registry> filter) {
        return sqlSession().selectList(NS + ".selectRegistries", filter);
    }

    public int count(FilterWrapper<Registry> filter) {
        return sqlSession().selectOne(NS + ".countRegistries", filter);
    }

    public void save(Registry registry) {
        sqlSession().insert(NS + ".insertRegistry", registry);

        for (Container container : registry.getContainers()) {
            container.setParentId(registry.getId());
            sqlSession().insert(NS + ".insertRegistryContainer", container);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateInNewTransaction(Registry registry) {
        update(registry);
    }

    public void update(Registry registry) {
        sqlSession().update(NS + ".updateRegistry", registry);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void delete(Registry registry) {
        sqlSession().delete(NS + ".deleteRegistryRecordContainers", registry.getId());
        sqlSession().delete(NS + ".deleteRegistryRecords", registry.getId());
        sqlSession().delete(NS + ".deleteRegistryContainers", registry.getId());
        sqlSession().delete(NS + ".deleteRegistry", registry.getId());
    }
}
