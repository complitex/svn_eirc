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
        return getSqlSessionManager().selectList(NS + ".selectRegistries", filter);
    }

    @Transactional
    public void save(Registry registry) {
        sqlSession().insert(NS + ".insertRegistry", registry);

        RegistryContainer registryContainer = new RegistryContainer(registry.getId());

        for (Container container : registry.getContainers()) {
            registryContainer.setContainer(container);
            sqlSession().insert(NS + ".insertRegistryContainer", registryContainer);
        }
    }

    public class RegistryContainer extends ContainerProxy {
        private Long registryId;

        public RegistryContainer(Long registryId) {
            this.registryId = registryId;
        }

        public Long getRegistryId() {
            return registryId;
        }
    }
}
