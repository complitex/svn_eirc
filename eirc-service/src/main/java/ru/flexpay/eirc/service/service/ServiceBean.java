package ru.flexpay.eirc.service.service;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.entity.Locale;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.Stateless;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceBean extends AbstractBean {

    private static final String NS = ServiceBean.class.getName();

    public static final String FILTER_MAPPING_ATTRIBUTE_NAME = "service";

    public void delete(Service service) {
        sqlSession().delete(NS + ".deleteServiceNames", service);
        sqlSession().delete(NS + ".deleteService", service);
    }

    public Service getService(long id) {
        return getService(null, id);
    }

    public Service getService(String dataSource, long id) {
        List<Service> resultOrderByDescData = (dataSource == null? sqlSession() : sqlSession(dataSource)).selectList(NS + ".selectService", id);
        return resultOrderByDescData.size() > 0? resultOrderByDescData.get(0): null;
    }

    public List<Service> getServices(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return sqlSession().selectList(NS + ".selectServicesByIds", ids);
    }

    @SuppressWarnings("unchecked")
    public List<Service> getServices(FilterWrapper<Service> filter) {
        if (filter != null && filter.getMap().containsKey("ids")) {
            return getServices((List<Long>)filter.getMap().get("ids"));
        }
        ServiceUtil.addFilterMappingObject(filter);
        if (filter != null && StringUtils.equals(filter.getSortProperty(), "id")) {
            filter.setSortProperty("service_id");
        }
        return sqlSession().selectList(NS + ".selectServices", filter);
    }

    public List<Service> getServices(String dataSource, FilterWrapper<Service> filter) {
        ServiceUtil.addFilterMappingObject(filter);
        if (filter != null && StringUtils.equals(filter.getSortProperty(), "id")) {
            filter.setSortProperty("service_id");
        }
        return sqlSession(dataSource).selectList(NS + ".selectServices", filter);
    }

    public int count(FilterWrapper<Service> filter) {
        ServiceUtil.addFilterMappingObject(filter);
        return sqlSession().selectOne(NS + ".countServices", filter);
    }

    public void save(Service service) {
        if (service.getId() == null) {
            insert(service);
        } else {
            update(service);
        }
    }

    private void insert(Service service) {
        sqlSession().insert(NS + ".insertService", service);
        for (Map.Entry<Locale, String> entry : service.getNames().entrySet()) {
            saveName(service, entry.getKey(), entry.getValue());
        }
    }

    private void update(Service service) {
        // update service
        Service oldObject = getService(service.getId());
        if (EqualsBuilder.reflectionEquals(oldObject, service)) {
            return;
        }
        sqlSession().update(NS + ".updateService", service);

        // update service`s name
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
            sqlSession().insert(NS + ".insertServiceName",
                ImmutableMap.<String, Object>of(
                        "serviceId", service.getId(),
                        "localeId",  locale.getId(),
                        "value",     value));
        }
    }

    private void updateName(Service service, Locale locale, String newValue, String oldValue) {
        if (StringUtils.isEmpty(newValue)) {
            deleteName(service, locale);
        } else if (!StringUtils.equals(newValue, oldValue)) {
            sqlSession().update(NS + ".updateServiceName",
                    ImmutableMap.<String, Object>of(
                            "serviceId", service.getId(),
                            "localeId",  locale.getId(),
                            "value",     newValue));
        }
    }

    private void deleteName(Service service, Locale locale) {
        sqlSession().delete(NS + ".deleteServiceName", ImmutableMap.<String, Object>of(
                "serviceId", service.getId(),
                "localeId",  locale.getId()));
    }

}
