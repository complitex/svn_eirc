package ru.flexpay.eirc.eirc_account.strategy;

import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;

import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;

/**
 * @author Pavel Sknar
 */
@Stateless
public class EircAccountStrategy extends AbstractBean {

    private static final String EIRC_ACCOUNT_MAPPING = EircAccountStrategy.class.getPackage().getName() + ".EircAccount";
    public static final String RESOURCE_BUNDLE = EircAccountStrategy.class.getName();

    @Transactional
    public void archive(EircAccount object, Date endDate) {

    }

    @Transactional
    public EircAccount findById(long id) {
       return sqlSession().selectOne(EIRC_ACCOUNT_MAPPING + ".getAddress", id);
    }

    public List<EircAccount> find(FilterWrapper<EircAccount> filter) {
        return sqlSession().selectList(EIRC_ACCOUNT_MAPPING + ".selectEircAccounts", filter);
    }

}
