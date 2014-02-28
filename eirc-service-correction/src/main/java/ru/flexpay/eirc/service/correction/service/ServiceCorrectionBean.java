package ru.flexpay.eirc.service.correction.service;

import org.complitex.dictionary.entity.Correction;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.AbstractBean;
import ru.flexpay.eirc.service.correction.entity.ServiceCorrection;

import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceCorrectionBean extends AbstractBean {
    public final static String NS = ServiceCorrectionBean.class.getName();
    private static final String NS_CORRECTION = Correction.class.getName();

    public ServiceCorrection geServiceCorrection(Long id){
        return sqlSession().selectOne(NS + ".selectServiceCorrection", id);
    }

    public List<ServiceCorrection> getServiceCorrections(FilterWrapper<ServiceCorrection> filterWrapper){
        return sqlSession().selectList(NS + ".selectServiceCorrections", filterWrapper);
    }

    public Integer getServiceCorrectionsCount(FilterWrapper<ServiceCorrection> filterWrapper){
        return sqlSession().selectOne(NS + ".selectServiceCorrectionsCount", filterWrapper);
    }

    public void save(ServiceCorrection serviceCorrection){
        if (serviceCorrection.getId() == null) {
            sqlSession().insert(NS_CORRECTION + ".insertCorrection", serviceCorrection);
        } else{
            sqlSession().update(NS_CORRECTION + ".updateCorrection", serviceCorrection);
        }
    }

    @Transactional
    public void delete(ServiceCorrection serviceCorrection){
        sqlSession().delete(NS_CORRECTION + ".deleteCorrection", serviceCorrection);
    }
}
