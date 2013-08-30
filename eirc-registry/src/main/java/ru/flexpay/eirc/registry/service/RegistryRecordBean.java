package ru.flexpay.eirc.registry.service;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;

import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryRecordBean extends AbstractBean {
    private static final String NS = RegistryRecordBean.class.getName();

    public List<Registry> getRegistryRecords(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRegistryRecords", filter);
    }

    @Transactional
    public void saveBulk(List<RegistryRecord> registryRecords) {
        SqlSession sqlSession = getSqlSessionManager().openSession(ExecutorType.BATCH);
        for (RegistryRecord registryRecord : registryRecords) {
            saveRegistryRecord(sqlSession, registryRecord);
        }
        sqlSession.flushStatements();

        for (RegistryRecord registryRecord : registryRecords) {
            saveRegistryRecordContainers(sqlSession, registryRecord);
        }
        sqlSession.flushStatements();
    }

    @Transactional
    public void save(RegistryRecord registryRecord) {
        SqlSession session = sqlSession();
        saveRegistryRecord(session, registryRecord);
        saveRegistryRecordContainers(session, registryRecord);
    }

    private void saveRegistryRecord(SqlSession session, RegistryRecord registryRecord) {
        session.insert(NS + ".insertRegistryRecord", registryRecord);
    }

    private void saveRegistryRecordContainers(SqlSession session, RegistryRecord registryRecord) {
        RegistryRecordContainer registryContainer = new RegistryRecordContainer(registryRecord.getId());

        for (Container container : registryRecord.getContainers()) {
            registryContainer.setContainer(container);
            session.insert(NS + ".insertRegistryRecordContainer", registryContainer);
        }
    }

    public class RegistryRecordContainer extends ContainerProxy {
        private Long registryRecordId;

        public RegistryRecordContainer(Long registryRecordId) {
            this.registryRecordId = registryRecordId;
        }

        public Long getRegistryRecordId() {
            return registryRecordId;
        }
    }
}
