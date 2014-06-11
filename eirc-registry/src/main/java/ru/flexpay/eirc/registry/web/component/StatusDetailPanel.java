package ru.flexpay.eirc.registry.web.component;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.RegistryRecordBean;

import javax.ejb.EJB;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Anatoly A. Ivanov java@inheaven.ru
 *         Date: 24.11.10 15:49
 */
public class StatusDetailPanel extends Panel {

    @EJB
    private RegistryRecordBean registryRecordBean;

    public StatusDetailPanel(String id, final IModel<RegistryRecordData> filterModel, final Component... update) {
        super(id);
        setOutputMarkupId(true);


        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container);

        IModel<List<StatusDetailInfo>> model = new LoadableDetachableModel<List<StatusDetailInfo>>() {

            @Override
            protected List<StatusDetailInfo> load() {
                return registryRecordBean.getStatusStatistics(filterModel.getObject());
            }
        };

        ListView<StatusDetailInfo> statusDetailsInfo = new ListView<StatusDetailInfo>("statusDetailsInfo", model) {

            @Override
            protected void populateItem(final ListItem<StatusDetailInfo> item) {
                final StatusDetailInfo statusDetailInfo = item.getModelObject();

                //Контейнер для ajax обновления вложенного списка
                final WebMarkupContainer importErrorsContainer = new WebMarkupContainer("importErrorsContainer");
                importErrorsContainer.setOutputMarkupPlaceholderTag(true);
                item.add(importErrorsContainer);

                final ListView<ImportErrorDetailInfo> importError = new ListView<ImportErrorDetailInfo>("importErrors") {

                    @Override
                    protected void populateItem(final ListItem<ImportErrorDetailInfo> item) {
                        final ImportErrorDetailInfo importErrorDetail = item.getModelObject();

                        //Контейнер для ajax обновления вложенного списка
                        final WebMarkupContainer addressErrorsContainer = new WebMarkupContainer("addressErrorsContainer");
                        addressErrorsContainer.setOutputMarkupPlaceholderTag(true);
                        item.add(addressErrorsContainer);

                        final ListView<ImportErrorDetail> addressError = new ListView<ImportErrorDetail>("addressErrors") {

                            @Override
                            protected void populateItem(ListItem<ImportErrorDetail> item) {
                                final ImportErrorDetail addressErrorDetail = item.getModelObject();

                                AjaxLink filter = new AjaxLink("expand") {

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        filterByImportError(statusDetailInfo, importErrorDetail, addressErrorDetail, filterModel);

                                        for (Component component : update) {
                                            target.add(component);
                                        }
                                    }
                                };
                                item.add(filter);

                                filter.add(new Label("name", renderAddress(addressErrorDetail.getDetails(), importErrorDetail.getImportErrorType()) + " (" + addressErrorDetail.getCount() + ")"));
                            }

                        };

                        AjaxLink expand = new IndicatingAjaxLink("expand") {

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                filterByImportErrorInfo(statusDetailInfo, importErrorDetail, filterModel);

                                for (Component component : update) {
                                    target.add(component);
                                }

                                if (statusDetailInfo.isImportErrorExist()) {
                                    addressErrorsContainer.setVisible(!addressErrorsContainer.isVisible());
                                    if (addressErrorsContainer.isVisible()) {
                                        RegistryRecord filterObject = new RegistryRecord(filterModel.getObject().getRegistryId());
                                        filterObject.setStatus(statusDetailInfo.getStatus());
                                        filterObject.setImportErrorType(importErrorDetail.getImportErrorType());
                                        addressError.setList(registryRecordBean.getAddressErrorStatistics(filterObject));
                                    } else {
                                        addressError.setList(Collections.<ImportErrorDetail>emptyList());
                                    }
                                    target.add(addressErrorsContainer);
                                }
                            }
                        };
                        item.add(expand);

                        expand.add(new Label("name", importErrorDetail.getImportErrorType().getLabel(getLocale()) + " (" + importErrorDetail.getCount() + ")"));

                        addressError.setOutputMarkupId(true);
                        addressErrorsContainer.setVisible(false);
                        addressErrorsContainer.add(addressError);
                    }

                };

                AjaxLink expand = new IndicatingAjaxLink("expand") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        for (Component component : update) {
                            target.add(component);
                        }

                        filterByStatusDetailInfo(statusDetailInfo, filterModel);

                        if (statusDetailInfo.isImportErrorExist()) {
                            importErrorsContainer.setVisible(!importErrorsContainer.isVisible());
                            if (importErrorsContainer.isVisible()) {
                                RegistryRecord filterObject = new RegistryRecord(filterModel.getObject().getRegistryId());
                                filterObject.setStatus(item.getModel().getObject().getStatus());
                                importError.setList(registryRecordBean.getImportErrorStatistics(filterObject));
                            } else {
                                importError.setList(Collections.<ImportErrorDetailInfo>emptyList());
                            }
                            target.add(importErrorsContainer);
                        }
                    }
                };
                item.add(expand);

                String info = statusDetailInfo.getStatus().getLabel(getLocale())
                        + " (" + statusDetailInfo.getCount() +")";
                expand.add(new Label("info", info));

                importError.setOutputMarkupId(true);
                importErrorsContainer.setVisible(false);
                importErrorsContainer.add(importError);
            }
        };

        container.add(statusDetailsInfo);
    }

    protected void filterByStatusDetailInfo(StatusDetailInfo statusDetailInfo, IModel<RegistryRecordData> filterModel) {
        RegistryRecord filterObject = new RegistryRecord(filterModel.getObject().getRegistryId());
        filterObject.setStatus(statusDetailInfo.getStatus());
        filterModel.setObject(filterObject);
    }

    protected void filterByImportErrorInfo(StatusDetailInfo statusDetailInfo, ImportErrorDetailInfo importErrorDetail,
                                           IModel<RegistryRecordData> filterModel) {
        filterByStatusDetailInfo(statusDetailInfo, filterModel);
        ((RegistryRecord)filterModel.getObject()).setImportErrorType(importErrorDetail.getImportErrorType());
    }

    protected void filterByImportError(StatusDetailInfo statusDetailInfo, ImportErrorDetailInfo importErrorDetail,
                                       ImportErrorDetail importError, IModel<RegistryRecordData> filterModel) {
        filterByStatusDetailInfo(statusDetailInfo, filterModel);
        CompoundPropertyModel<RegistryRecordData> compoundPropertyModel = new CompoundPropertyModel<>(filterModel);
        for (Map.Entry<String, String> entry : importError.getDetails().entrySet()) {
            IModel<String> property = compoundPropertyModel.bind(entry.getKey());
            property.setObject(entry.getValue());
        }
    }

    private String renderAddress(Map<String, String> map, ImportErrorType importErrorType) {
        if (map == null) {
            return "";
        }
        switch (importErrorType) {
            case CITY_UNRESOLVED:
            case MORE_ONE_CITY:
            case MORE_ONE_CITY_CORRECTION:
                return map.get("city");

            case STREET_TYPE_UNRESOLVED:
            case MORE_ONE_STREET_TYPE:
            case MORE_ONE_STREET_TYPE_CORRECTION:
                return map.get("streetType");

            case STREET_UNRESOLVED:
            case STREET_AND_BUILDING_UNRESOLVED:
            case MORE_ONE_STREET:
            case MORE_ONE_STREET_CORRECTION:
                return StringUtils.join(ImmutableList.of(map.get("city"), map.get("streetType"), map.get("street")), ",");

            case BUILDING_UNRESOLVED:
            case MORE_ONE_BUILDING:
            case MORE_ONE_BUILDING_CORRECTION:
                return StringUtils.join(
                        ImmutableList.of(map.get("city"), map.get("streetType"), map.get("street"), map.get("buildingNumber"), map.get("buildingCorp")),
                        ",");

            case APARTMENT_UNRESOLVED:
            case MORE_ONE_APARTMENT:
            case MORE_ONE_APARTMENT_CORRECTION:
                return StringUtils.join(
                        ImmutableList.of(map.get("city"), map.get("streetType"), map.get("street"), map.get("buildingNumber"),
                                map.get("buildingCorp"), map.get("apartment")),
                        ",");

            case ROOM_UNRESOLVED:
            case MORE_ONE_ROOM:
            case MORE_ONE_ROOM_CORRECTION:
                return StringUtils.join(
                        ImmutableList.of(map.get("city"), map.get("streetType"), map.get("street"), map.get("buildingNumber"),
                                map.get("buildingCorp"), map.get("apartment"), map.get("room")),
                        ",");

        }
        return "";
    }
}
