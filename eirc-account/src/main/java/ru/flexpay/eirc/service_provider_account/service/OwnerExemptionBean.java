package ru.flexpay.eirc.service_provider_account.service;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import org.complitex.dictionary.util.DateUtil;
import ru.flexpay.eirc.service_provider_account.entity.OwnerExemption;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class OwnerExemptionBean extends AbstractBean {

    private static final String NS = OwnerExemptionBean.class.getPackage().getName() + ".OwnerExemptionBean";
    public static final String ENTITY_TABLE = "owner_exemption";

    @EJB
    private SequenceBean sequenceBean;

    public void archive(OwnerExemption object) {
        if (object.getEndDate() == null) {
            object.setEndDate(DateUtil.getCurrentDate());
        }
        sqlSession().update(NS + ".updateOwnerExemptionEndDate", object);
    }

    public OwnerExemption getOwnerExemption(long id) {
        List<OwnerExemption> resultOrderByDescData = sqlSession().selectList(NS + ".selectOwnerExemption", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<OwnerExemption> getOwnerExemptions(FilterWrapper<OwnerExemption> filter) {
        return sqlSession().selectList(NS + ".selectOwnerExemptions", filter);
    }

    public List<OwnerExemption> getOwnerExemptionsWithExemptions(FilterWrapper<OwnerExemption> filter) {
        return sqlSession().selectList(NS + ".selectOwnerExemptionsWithExemptions", filter);
    }

    public int count(FilterWrapper<OwnerExemption> filter) {
        return sqlSession().selectOne(NS + ".countOwnerExemptions", filter);
    }

    public void save(OwnerExemption ownerExemption) {
        if (ownerExemption.getId() == null) {
            saveNew(ownerExemption);
        } else {
            update(ownerExemption);
        }
    }

    private void saveNew(OwnerExemption ownerExemption) {
        ownerExemption.setId(sequenceBean.nextId(ENTITY_TABLE));
        sqlSession().insert(NS + ".insertOwnerExemption", ownerExemption);
    }

    private void update(OwnerExemption ownerExemption) {
        OwnerExemption oldObject = getOwnerExemptionByPkId(ownerExemption.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, ownerExemption)) {
            return;
        }
        archive(oldObject);
        ownerExemption.setBeginDate(oldObject.getEndDate());
        sqlSession().insert(NS + ".insertOwnerExemption", ownerExemption);
    }

    public OwnerExemption getOwnerExemptionByPkId(long pkId) {
        return sqlSession().selectOne(NS + ".selectOwnerExemptionByPkId", pkId);
    }
    
}
