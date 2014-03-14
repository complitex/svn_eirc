package ru.flexpay.eirc.dictionary.web;

import com.google.common.collect.ImmutableList;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.strategy.StrategyFactory;
import org.complitex.dictionary.web.component.ShowMode;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import org.complitex.dictionary.web.component.search.SearchComponentState;
import ru.flexpay.eirc.dictionary.entity.Address;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class CollapsibleInputSearchComponent extends org.complitex.dictionary.web.component.search.CollapsibleInputSearchComponent {

    private static final List<String> SEARCH_FILTERS = ImmutableList.of("country", "region", "city", "street", "building", "apartment", "room");

    private static final List<String> ADDRESS_DESCRIPTION = ImmutableList.of("street", "city", "region", "country");

    @EJB
    private StrategyFactory strategyFactory;

    public CollapsibleInputSearchComponent(String id, SearchComponentState searchComponentState, ISearchCallback callback, ShowMode showMode, boolean enabled) {
        super(id, searchComponentState, SEARCH_FILTERS, callback, showMode, enabled);
    }

    @Override
    protected void init() {
        initSearchComponentState(getSearchComponentState(), getAddress());
        super.init();
    }

    protected abstract Address getAddress();

    private void initSearchComponentState(SearchComponentState componentState, Address address) {
        if (address == null) {
            return;
        }
        componentState.clear();

        DomainObject room = null;
        DomainObject apartment = null;
        DomainObject building = null;

        switch (address.getEntity()) {
            case ROOM:
                room = findObject(address.getId(), "room");
                componentState.put("room", room);
            case APARTMENT:
                Long apartmentId = null;
                if (room != null && room.getParentEntityId() == 100) {
                    apartmentId = room.getParentId();
                } else if (room == null) {
                    apartmentId = address.getId();
                }
                if (apartmentId != null) {
                    apartment = findObject(apartmentId, "apartment");
                    componentState.put("apartment", apartment);
                }
            case BUILDING:
                Long buildId;
                if (apartment != null) {
                    buildId = apartment.getParentId();
                } else if (room != null) {
                    buildId = room.getParentId();
                } else {
                    buildId = address.getId();
                }

                building = findObject(buildId, "building");
                componentState.put("building", building);
                break;
        }

        if (building == null) {
            throw new RuntimeException("Failed EIRC Account`s address");
        }

        DomainObject child = findObject(building.getParentId(), "building_address");

        for (String desc : ADDRESS_DESCRIPTION) {
            child = findObject(child.getParentId(), desc);
            componentState.put(desc, child);
        }
    }

    private DomainObject findObject(Long objectId, String entity) {
        IStrategy strategy = strategyFactory.getStrategy(entity);
        return strategy.findById(objectId, true);
    }
}
