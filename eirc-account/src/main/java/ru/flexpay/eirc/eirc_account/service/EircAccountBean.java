package ru.flexpay.eirc.eirc_account.service;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class EircAccountBean extends AbstractBean {

    private static final String NS = EircAccountBean.class.getPackage().getName() + ".EircAccountBean";
    public static final String ENTITY_TABLE = "eirc_account";

    private static final List<String> searchFilters = ImmutableList.of("city", "street", "building", "apartment", "room");

    @EJB
    private SequenceBean sequenceBean;

    @Transactional
    public void archive(EircAccount object) {
        if (object.getEndDate() == null) {
            object.setEndDate(new Date());
        }
        sqlSession().update("updateEndDate", object);
    }

    public EircAccount getEircAccount(long id) {
        List<EircAccount> resultOrderByDescData = sqlSession().selectList(NS + ".selectEircAccount", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<EircAccount> getEircAccounts(FilterWrapper<EircAccount> filter) {
        return sqlSession().selectList(NS + ".selectEircAccounts", filter);
    }

    public int count(FilterWrapper<EircAccount> filter) {
        return sqlSession().selectOne(NS + ".countEircAccounts", filter);
    }

    @Transactional
    public void save(EircAccount eircAccount) {
        eircAccount.setId(sequenceBean.nextId(ENTITY_TABLE));
        sqlSession().insert(NS + ".insert", eircAccount);
    }

    @Transactional
    public void update(EircAccount eircAccount) {
        EircAccount oldObject = getEricAccountByPkId(eircAccount.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, eircAccount)) {
            return;
        }
        archive(oldObject);
        eircAccount.setBeginDate(oldObject.getEndDate());
        sqlSession().insert(NS + ".insert", eircAccount);
    }

    public EircAccount getEricAccountByPkId(long pkId) {
        return sqlSession().selectOne(NS + ".selectEircAccountByPkId", pkId);
    }

    public List<String> getSearchFilters() {
        return searchFilters;
    }

}
