package ru.flexpay.eirc.organization_type.strategy;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.ejb.Stateless;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.organization_type.strategy.OrganizationTypeStrategy;

/**
 *
 * @author Artem
 */
@Stateless
public class EircOrganizationTypeStrategy extends OrganizationTypeStrategy {

    private static final String STRATEGY_NAME = EircOrganizationTypeStrategy.class.getSimpleName();
    /**
     * Organization type ids
     */
    public static final long SERVICE_PROVIDER = 2;

    @Override
    protected Collection<Long> getReservedInstanceIds() {
        return ImmutableList.of(USER_ORGANIZATION_TYPE, SERVICE_PROVIDER);
    }

    @Override
    public PageParameters getEditPageParams(Long objectId, Long parentId, String parentEntity) {
        PageParameters pageParameters = super.getEditPageParams(objectId, parentId, parentEntity);
        pageParameters.set(STRATEGY, STRATEGY_NAME);
        return pageParameters;
    }

    @Override
    public PageParameters getHistoryPageParams(long objectId) {
        PageParameters pageParameters = super.getHistoryPageParams(objectId);
        pageParameters.set(STRATEGY, STRATEGY_NAME);
        return pageParameters;
    }

    @Override
    public PageParameters getListPageParams() {
        PageParameters pageParameters = super.getListPageParams();
        pageParameters.set(STRATEGY, STRATEGY_NAME);
        return pageParameters;
    }
}
