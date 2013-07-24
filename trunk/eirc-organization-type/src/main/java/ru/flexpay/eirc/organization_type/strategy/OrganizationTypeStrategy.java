package ru.flexpay.eirc.organization_type.strategy;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.ejb.Stateless;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import ru.flexpay.eirc.organization_type.entity.OrganizationType;

/**
 *
 * @author Artem
 */
@Stateless
public class OrganizationTypeStrategy extends org.complitex.organization_type.strategy.OrganizationTypeStrategy {

    private static final String STRATEGY_NAME = OrganizationTypeStrategy.class.getSimpleName();

    @Override
    protected Collection<Long> getReservedInstanceIds() {
        ImmutableList.Builder<Long> builder = ImmutableList.builder();
        for (OrganizationType organizationType : OrganizationType.values()) {
            builder.add(organizationType.getId());
        }
        return builder.build();
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
