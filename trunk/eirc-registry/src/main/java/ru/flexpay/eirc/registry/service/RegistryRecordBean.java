package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.ibatis.session.SqlSession;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.registry.entity.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryRecordBean extends AbstractBean {
    private static final String NS = RegistryRecordBean.class.getName();

    public static final String OPERATION_DATE_RANGE = "operationDateRange";

    public List<RegistryRecordData> getRegistryRecords(FilterWrapper<RegistryRecordData> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRegistryRecords", filter);
    }

    public int count(FilterWrapper<RegistryRecordData> filter) {
        return getSqlSessionManager().selectOne(NS + ".countRegistryRecords", filter);
    }

    /**
     * Get registry records to linking
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecordData> getRecordsToLinking(FilterWrapper<RegistryRecordData> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRecordsToLinking", filter);
    }

    /**
     * Get registry records to linking
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecordData> getCorrectionRecordsToLinking(FilterWrapper<RegistryRecordData> filter) {
        return getSqlSessionManager().selectList(NS + ".selectCorrectionRecordsToLinking", filter);
    }

    /**
     * Get registry records to processing
     * @param filter Filter must content registryId, count, first
     * @return registry records
     */
    public List<RegistryRecordData> getRecordsToProcessing(FilterWrapper<RegistryRecordData> filter) {
        return getSqlSessionManager().selectList(NS + ".selectRecordsToProcessing", filter);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createBulk(List<RegistryRecordData> registryRecords) {
        Map<String, Object> params = createBulkRecords(registryRecords);
        List<Container> containers = Lists.newArrayList();
        long id = ((Long)params.get("id"));
        createBulkRecordContainers(registryRecords, containers, id);

        /*
        SqlSession session = sqlSession();

        for (RegistryRecordData registryRecord : registryRecords) {
            saveRegistryRecord(session, registryRecord);
        }

        session.flushStatements();

        for (RegistryRecordData registryRecord : registryRecords) {
            saveRegistryRecordContainers(session, registryRecord);
        }*/

    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void createBulkRecordContainers(List<RegistryRecordData> registryRecords, List<Container> containers, long id) {
        for (RegistryRecordData registryRecord : registryRecords) {
            for (Container container : registryRecord.getContainers()) {
                container.setParentId(id);
                containers.add(container);
            }
            id++;
        }
        getSqlSessionManager().insert(NS + ".insertRegistryRecordContainers", containers);
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Map<String, Object> createBulkRecords(List<RegistryRecordData> registryRecords) {
        for (RegistryRecordData registryRecord : registryRecords) {
            registryRecord.getUniqueOperationNumber();
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("registryRecords", registryRecords);
        params.put("id", 0L);
        getSqlSessionManager().insert(NS + ".insertRegistryRecords", params);
        return params;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void updateBulk(List<RegistryRecordData> registryRecords) {
        SqlSession session = sqlSession();

        for (RegistryRecordData registryRecord : registryRecords) {
            saveRegistryRecord(session, registryRecord);
        }

    }

    public void save(RegistryRecordData registryRecord) {
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
    private boolean saveRegistryRecord(SqlSession session, RegistryRecordData registryRecord) {
        if (registryRecord.getId() == null) {
            session.insert(NS + ".insertRegistryRecord", registryRecord);
            return true;
        }
        session.update(NS + ".updateRegistryRecord", registryRecord);
        return false;
    }

    private void saveRegistryRecordContainers(SqlSession session, RegistryRecordData registryRecord) {

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

    public List<StatusDetailInfo> getStatusStatistics(RegistryRecordData registryRecord) {
        return sqlSession().selectList(NS + ".selectStatusStatistics", registryRecord);
    }

    public List<ImportErrorDetailInfo> getImportErrorStatistics(RegistryRecordData registryRecord) {
        return sqlSession().selectList(NS + ".selectImportErrorStatistics", registryRecord);
    }

    public List<ImportErrorDetail> getAddressErrorStatistics(RegistryRecordData registryRecord) {
        return sqlSession().selectList(NS + ".selectAddressErrorStatistics", registryRecord);
    }
}
