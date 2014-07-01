package ru.flexpay.eirc.eirc_account.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.ServiceProviderAccountBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class EircAccountBean extends AbstractBean {

    private static final String NS = EircAccountBean.class.getPackage().getName() + ".EircAccountBean";
    public static final String ENTITY_TABLE = "eirc_account";

    public static final String FILTER_MAPPING_ATTRIBUTE_NAME = "eircAccount";

    private static final List<String> searchFilters = ImmutableList.of("country", "region", "city", "street", "building", "apartment", "room");

    private static final CheckDigit CHECK_DIGIT = EAN13CheckDigit.EAN13_CHECK_DIGIT;
    @EJB
    private SequenceBean sequenceBean;

    @EJB
    private ServiceProviderAccountBean serviceProviderAccountBean;

    public String generateEircAccountNumber(String prefix) throws NumberFormatException, CheckDigitException {
        long lPrefix = Long.parseLong(prefix);
        long id = sequenceBean.nextIdOrInit(prefix);
        String accountNumber = String.valueOf(lPrefix*100000000 + id);

        return accountNumber + CHECK_DIGIT.calculate(accountNumber);
    }

    public void archive(EircAccount object) {
        archive(object, true);
    }

    private void archive(EircAccount object, boolean withSPA) {
        if (object.getEndDate() == null) {
            object.setEndDate(new Date());
        }
        if (withSPA) {
            List<ServiceProviderAccount> serviceProviderAccounts =
                    serviceProviderAccountBean.getServiceProviderAccounts(FilterWrapper.of(new ServiceProviderAccount(new EircAccount(object.getId()))));
            for (ServiceProviderAccount serviceProviderAccount : serviceProviderAccounts) {
                serviceProviderAccountBean.archive(serviceProviderAccount);
            }
        }
        sqlSession().update("updateEndDate", object);
    }

    public EircAccount getEircAccount(long id) {
        List<EircAccount> resultOrderByDescData = sqlSession().selectList(NS + ".selectEircAccount", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public EircAccount getEircAccount(Address address) {
        return sqlSession().selectOne(NS + ".selectEircAccountByAddress", address);
    }

    public Boolean eircAccountExists(Long eircAccountId, String accountNumber) {
        Map<String, Object> params = Maps.newHashMap();
        if (eircAccountId != null) {
            params.put("objectId", eircAccountId);
        }
        params.put("accountNumber", accountNumber);
        return sqlSession().<Integer>selectOne(NS + ".eircAccountNumberExists", params) != null;
    }

    public Boolean eircAccountExists(Long eircAccountId, Address address) {
        Map<String, Object> params = Maps.newHashMap();
        if (eircAccountId != null) {
            params.put("objectId", eircAccountId);
        }
        params.put("addressId", address.getId());
        params.put("addressEntityId", address.getEntity().getId());
        return sqlSession().<Integer>selectOne(NS + ".eircAccountByAddressExists", params) != null;
    }

    public List<EircAccount> getEircAccounts(FilterWrapper<EircAccount> filter) {
        EircAccountUtil.addFilterMappingObject(filter);
        return sqlSession().selectList(NS + ".selectEircAccounts", filter);
    }

    public int count(FilterWrapper<EircAccount> filter) {
        EircAccountUtil.addFilterMappingObject(filter);
        return sqlSession().selectOne(NS + ".countEircAccounts", filter);
    }

    public void save(EircAccount eircAccount) {
        if (eircAccount.getId() == null) {
            saveNew(eircAccount);
        } else {
            update(eircAccount);
        }
    }

    private void saveNew(EircAccount eircAccount) {
        eircAccount.setId(sequenceBean.nextId(ENTITY_TABLE));
        sqlSession().insert(NS + ".insertEircAccount", eircAccount);
    }

    private void update(EircAccount eircAccount) {
        EircAccount oldObject = getEricAccountByPkId(eircAccount.getPkId());
        if (EqualsBuilder.reflectionEquals(oldObject, eircAccount)) {
            return;
        }
        archive(oldObject, false);
        eircAccount.setBeginDate(oldObject.getEndDate());
        sqlSession().insert(NS + ".insertEircAccount", eircAccount);
    }

    public EircAccount getEricAccountByPkId(long pkId) {
        return sqlSession().selectOne(NS + ".selectEircAccountByPkId", pkId);
    }

    public List<String> getSearchFilters() {
        return searchFilters;
    }

}
