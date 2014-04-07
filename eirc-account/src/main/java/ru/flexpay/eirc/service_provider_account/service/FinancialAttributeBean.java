package ru.flexpay.eirc.service_provider_account.service;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.service_provider_account.entity.FinancialAttribute;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class FinancialAttributeBean<T extends FinancialAttribute> extends AbstractBean {

    private static final String NS = ru.flexpay.eirc.service_provider_account.service.FinancialAttributeBean.class.getName();

    @Transactional
    public void delete(T financialAttribute) {
        sqlSession().delete("delete", financialAttribute);
    }

    public T getFinancialAttribute(long id) {
        List<T> resultOrderByDescData = sqlSession().selectList(getNameSpace() + ".selectFinancialAttribute", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<T> getFinancialAttributes(FilterWrapper<T> filter) {
        return sqlSession().selectList(getNameSpace() + ".selectFinancialAttributes", filter);
    }

    public int count(FilterWrapper<T> filter) {
        return sqlSession().selectOne(getNameSpace() + ".countFinancialAttributes", filter);
    }

    @Transactional
    public void save(T financialAttribute) {
        if (financialAttribute.getId() == null) {
            insert(financialAttribute);
        } else {
            update(financialAttribute);
        }
    }

    private void insert(T financialAttribute) {
        sqlSession().insert(getNameSpace() + ".insertFinancialAttribute", financialAttribute);
    }

    private void update(T financialAttribute) {
        // update financialAttribute
        T oldObject = getFinancialAttribute(financialAttribute.getId());
        if (EqualsBuilder.reflectionEquals(oldObject, financialAttribute)) {
            return;
        }
        sqlSession().update(getNameSpace() + ".updateFinancialAttribute", financialAttribute);
    }

    public abstract T getInstance();

    protected String getNameSpace() {
        return NS;
    }
}
                                             