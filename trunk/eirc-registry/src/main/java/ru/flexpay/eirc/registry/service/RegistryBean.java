package ru.flexpay.eirc.registry.service;

import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;

import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryBean extends AbstractBean {
    private static final String NS = RegistryBean.class.getName();

    public List<Registry> getRegistries(FilterWrapper<Registry> filter) {
        return sqlSession().selectList(NS + ".selectRegistries", filter);
    }

    public int count(FilterWrapper<Registry> filter) {
        return sqlSession().selectOne(NS + ".countRegistries", filter);
    }

    @Transactional
    public void save(Registry registry) {
        sqlSession().insert(NS + ".insertRegistry", registry);

        for (Container container : registry.getContainers()) {
            container.setParentId(registry.getId());
            sqlSession().insert(NS + ".insertRegistryContainer", container);
        }
    }

    public void update(Registry registry) {
        sqlSession().update(NS + ".updateRegistry", registry);
    }
}
