package ru.flexpay.eirc.eirc_account.strategy;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import org.complitex.dictionary.util.ResourceUtil;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@Stateless
public class EircAccountStrategy extends AbstractBean {

    private static final String EIRC_ACCOUNT_MAPPING = EircAccountStrategy.class.getPackage().getName() + ".EircAccount";
    public static final String RESOURCE_BUNDLE = EircAccountStrategy.class.getName();

    @EJB
    private SequenceBean sequenceBean;

    @Transactional
    public void archive(EircAccount object) {
        if (object.getEndDate() == null) {
            object.setEndDate(new Date());
        }
        sqlSession().update("updateEndDate", object);
    }

    public EircAccount findById(long id) {
       return sqlSession().selectOne(EIRC_ACCOUNT_MAPPING + ".getEircAccount", id);
    }

    public List<EircAccount> find(FilterWrapper<EircAccount> filter) {
        return sqlSession().selectList(EIRC_ACCOUNT_MAPPING + ".selectEircAccounts", filter);
    }

    @Transactional
    public void insert(EircAccount eircAccount) {
        eircAccount.setId(sequenceBean.nextId(getEntityTable()));
        sqlSession().insert(EIRC_ACCOUNT_MAPPING + ".insert", eircAccount);
    }

    @Transactional
    public void update(EircAccount eircAccount) {
        EircAccount oldObject = findByPkId(eircAccount.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, eircAccount)) {
            return;
        }
        if (oldObject.getBeginDate().after(eircAccount.getBeginDate())) {
            throw new RuntimeException("Can not update EIRC Account. Fail date. Old Object: " + oldObject +
                    ", New Object: " + eircAccount + ".");
        }
        oldObject.setEndDate(eircAccount.getBeginDate());
        archive(oldObject);
        insert(eircAccount);
    }

    public static String getEntityTable() {
        return "eirc_account";
    }

    private EircAccount findByPkId(long pkId) {
        return sqlSession().selectOne(EIRC_ACCOUNT_MAPPING + ".getEircAccount", pkId);
    }

    public PageParameters getEditPageParams(Long objectId, Long parentId, String parentEntity) {
        PageParameters pageParameters = new PageParameters();
        return pageParameters;
    }

    public PageParameters getHistoryPageParams(long objectId) {
        PageParameters pageParameters = new PageParameters();
        return pageParameters;
    }

    public PageParameters getListPageParams() {
        PageParameters pageParameters = new PageParameters();
        return pageParameters;
    }

    public Class<? extends WebPage> getListPage() {
        return null;
    }

    public String getPluralEntityLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, getEntityTable(), locale);
    }

}
