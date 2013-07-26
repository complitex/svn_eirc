package ru.flexpay.eirc.eirc_account.strategy;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.string.Strings;
import org.complitex.address.strategy.building.web.list.BuildingList;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.example.AttributeExample;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.strategy.web.DomainObjectListPanel;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
public class EircAccountStrategy {

    @Transactional
    public void archive(EircAccount object, Date endDate) {

    }

    @Transactional
    public EircAccount findById(long id) {
        return null;
    }

    private static void configureExampleImpl(DomainObjectExample example, Map<String, Long> ids, String searchTextInput) {

        Long apartmentId = ids.get("apartment");
        if (apartmentId != null && apartmentId > 0) {
            example.setParentId(apartmentId);
            example.setParentEntity("apartment");
        } else {
            Long buildingId = ids.get("building");
            if (buildingId != null && buildingId > 0) {
                example.setParentId(buildingId);
                example.setParentEntity("building");
            } else {
                example.setParentId(-1L);
                example.setParentEntity("");
            }
        }
    }

    private static class SearchCallback implements ISearchCallback, Serializable {

        @Override
        public void found(Component component, Map<String, Long> ids, AjaxRequestTarget target) {
            DomainObjectListPanel list = component.findParent(DomainObjectListPanel.class);
            configureExampleImpl(list.getExample(), ids, null);
            list.refreshContent(target);
        }
    }

    public ISearchCallback getSearchCallback() {
        return new SearchCallback();
    }

}
