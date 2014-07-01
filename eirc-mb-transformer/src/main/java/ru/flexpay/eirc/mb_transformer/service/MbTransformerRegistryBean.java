package ru.flexpay.eirc.mb_transformer.service;

import com.google.common.collect.Maps;
import org.complitex.dictionary.service.AbstractBean;

import javax.ejb.Stateless;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class MbTransformerRegistryBean extends AbstractBean {
    private static final String NS = MbTransformerRegistryBean.class.getName();

    public Long generateRegistryNumber() {
        Map<String, Object> result = Maps.newHashMap();

        sqlSession().insert(NS + ".insertRegistryNumber", result);

        Long id = (Long)result.get("id");

        sqlSession().delete(NS + ".deleteRegistryNumber", id);

        return id;
    }

}
