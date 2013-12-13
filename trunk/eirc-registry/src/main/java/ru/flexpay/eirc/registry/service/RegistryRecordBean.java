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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

    /**
     * Get registry records to linking
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecord> getRecordsToLinking(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRecordsToLinking", filter);
    }

    /**
     * Get registry records to linking
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecord> getCorrectionRecordsToLinking(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectList(NS + ".selectCorrectionRecordsToLinking", filter);
    }

    /**
     * Get registry records to processing
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecord> getRecordsToProcessing(FilterWrapper<RegistryRecord> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRecordsToProcessing", filter);
    }

    @Transactional(executorType = ExecutorType.BATCH)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createBulk(List<RegistryRecord> registryRecords) {
        SqlSession session = sqlSession();

        for (RegistryRecord registryRecord : registryRecords) {
            saveRegistryRecord(session, registryRecord);
        }

        session.flushStatements();

        for (RegistryRecord registryRecord : registryRecords) {
            saveRegistryRecordContainers(session, registryRecord);
        }

    }

    @Transactional(executorType = ExecutorType.BATCH)
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void updateBulk(List<RegistryRecord> registryRecords) {
        SqlSession session = sqlSession();

        for (RegistryRecord registryRecord : registryRecords) {
            saveRegistryRecord(session, registryRecord);
        }

    }

    @Transactional
    public void save(RegistryRecord registryRecord) {
        SqlSession session = sqlSession();
        if (saveRegistryRecord(session, registryRecord)) {
            saveRegistryRecordContainers(session, registryRecord);
        }
    }

    /**
     * Save registry record
     *
     * @param session Sql session
     * @param registryRecord Registry record
     * @return <code>true</code> if registry record was created otherwise <code>false</code>
     */
    private boolean saveRegistryRecord(SqlSession session, RegistryRecord registryRecord) {
        if (registryRecord.getId() == null) {
            session.insert(NS + ".insertRegistryRecord", registryRecord);
            return true;
        }
        session.update(NS + ".updateRegistryRecord", registryRecord);
        return false;
    }

    private void saveRegistryRecordContainers(SqlSession session, RegistryRecord registryRecord) {

        for (Container container : registryRecord.getContainers()) {
            container.setParentId(registryRecord.getId());
            session.insert(NS + ".insertRegistryRecordContainer", container);
        }
    }

    public boolean hasRecordsToLinking(Registry registry) {
        return sqlSession().selectOne(NS + ".hasRecordsToLinking", registry.getId()) != null;
    }

    public boolean hasRecordsToProcessing(Registry registry) {
        return sqlSession().selectOne(NS + ".hasRecordsToProcessing", registry.getId()) != null;
    }
}
