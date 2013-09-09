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

    public List<RegistryRecord> getRegistryRecords(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRegistryRecords", filter);
    }

    public int count(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectOne(NS + ".countRegistryRecords", filter);
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

        for (Container container : registryRecord.getContainers()) {
            container.setParentId(registryRecord.getId());
            session.insert(NS + ".insertRegistryRecordContainer", container);
        }
    }

    public boolean hasRecordsToProcessing(Registry registry) {
        return sqlSession().selectOne(NS + ".hasRecordsToProcessing", registry.getId()) != null;
    }
}
