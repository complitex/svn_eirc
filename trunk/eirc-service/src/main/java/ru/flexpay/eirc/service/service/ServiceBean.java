package ru.flexpay.eirc.service.service;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import org.complitex.dictionary.service.SequenceBean;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceBean extends AbstractBean {

    private static final String NS = ServiceBean.class.getPackage().getName() + ".ServiceBean";

    private static final String ENTITY_LOCALIZED_TABLE = "service_string_culture";

    @EJB
    private SequenceBean sequenceBean;

    @Transactional
    public void delete(Service service) {
        sqlSession().delete("delete", service);
        for (Map.Entry<Locale, String> entry : service.getNames().entrySet()) {
            deleteName(service, entry.getKey());
        }
    }

    public Service getService(long id) {
        List<Service> resultOrderByDescData = sqlSession().selectList(NS + ".selectService", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<Service> getServices(FilterWrapper<Service> filter) {
        return sqlSession().selectList(NS + ".selectServices", filter);
    }

    public int count(FilterWrapper<Service> filter) {
        return sqlSession().selectOne(NS + ".countServices", filter);
    }

    @Transactional
    public void save(Service service) {
        service.setNameId(sequenceBean.nextId(ENTITY_LOCALIZED_TABLE));
        for (Map.Entry<Locale, String> entry : service.getNames().entrySet()) {
            saveName(service, entry.getKey(), entry.getValue());
        }
        sqlSession().insert(NS + ".insert", service);
    }

    @Transactional
    public void update(Service service) {
        Service oldObject = getService(service.getId());
        if (EqualsBuilder.reflectionEquals(oldObject, service)) {
            return;
        }
        sqlSession().update(NS + ".update", service);

        Map<Locale, String> oldNames = oldObject.getNames();
        for (Map.Entry<Locale, String> entry : service.getNames().entrySet()) {
            if (oldNames.containsKey(entry.getKey())) {
                updateName(service, entry.getKey(), entry.getValue(), oldNames.get(entry.getKey()));
            } else {
                saveName(service, entry.getKey(), entry.getValue());
            }
        }
    }

    private void saveName(Service service, Locale locale, String value) {
        if (StringUtils.isNotEmpty(value)) {
            sqlSession().insert(NS + ".insertName",
                ImmutableMap.<String, Object>of(
                        "id",       service.getNameId(),
                        "localeId", locale.getId(),
                        "value",    value));
        }
    }

    private void updateName(Service service, Locale locale, String newValue, String oldValue) {
        if (StringUtils.isEmpty(newValue)) {
            deleteName(service, locale);
        } else if (!StringUtils.equals(newValue, oldValue)) {
            sqlSession().update(NS + ".updateName",
                    ImmutableMap.<String, Object>of(
                            "id",       service.getNameId(),
                            "localeId", locale.getId(),
                            "value",    newValue));
        }
    }

    private void deleteName(Service service, Locale locale) {
        sqlSession().delete(NS + ".deleteName", ImmutableMap.<String, Object>of(
                "id",       service.getNameId(),
                "localeId", locale.getId()));
    }

}
