package ru.flexpay.eirc.organization.web.edit;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.util.CloneUtil;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.organization.web.edit.component.ActionPanel;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.web.component.ServiceDialog;

import javax.ejb.EJB;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class ServiceAllowableListPanel extends Panel {

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private LocaleBean localeBean;

    private List<Attribute> services;

    private DataTable<Attribute, String> table;

    private final List<IColumn<Attribute, String>> COLUMNS = ImmutableList.<IColumn<Attribute, String>>of(
            new AbstractColumn<Attribute, String>(new ResourceModel("name")) {

                @Override
                public void populateItem(Item<ICellPopulator<Attribute>> cellItem, String componentId, IModel<Attribute> rowModel) {

                    Service service = serviceBean.getService(rowModel.getObject().getValueId());
                    cellItem.add(new Label(componentId, service.getName(localeBean.convert(getLocale()))));
                }
            },
            new AbstractColumn<Attribute, String>(new ResourceModel("action")) {

                @Override
                public void populateItem(Item<ICellPopulator<Attribute>> cellItem, String componentId, final IModel<Attribute> rowModel) {
                    cellItem.add(new ActionPanel(componentId, new ResourceModel("delete")) {
                        @Override
                        protected void doAction(AjaxRequestTarget target) {
                            services.remove(rowModel.getObject());
                            target.add(table);
                        }
                    });
                }
            }
    );

    public ServiceAllowableListPanel(String id, Organization organization) {
        super(id);
        services = organization.getAttributes(EircOrganizationStrategy.SERVICE);

        final Attribute newService = getNewService(services);

        table = new DataTable<>("services", COLUMNS, new ListDataProvider<>(services), 1000);
        table.setOutputMarkupId(true);
        add(table);

        final IModel<Service> serviceModel = new IModel<Service>() {
            @Override
            public Service getObject() {
                return null;
            }

            @Override
            public void setObject(Service object) {
                boolean contentValue = false;
                for (Attribute attribute : services) {
                    if (attribute.getValueId().equals(object.getId())) {
                        contentValue = true;
                    }
                }
                if (!contentValue) {
                    Attribute addedAttribute = CloneUtil.cloneObject(newService);
                    addedAttribute.setValueId(object.getId());

                    services.add(addedAttribute);
                }
            }

            @Override
            public void detach() {

            }
        };

        final ServiceDialog serviceDialog = new ServiceDialog("serviceDialog", serviceModel, true, table);
        add(serviceDialog);

        AjaxLink addButton = new AjaxLink("add") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                serviceDialog.open(target);
            }
        };
        add(addButton);

    }

    private Attribute getNewService(List<Attribute> services) {
        Attribute newService = null;

        for (Attribute service : services) {
            if (service.getValueId() == null) {
                newService = service;
                break;
            }
        }
        if (newService != null) {
            services.remove(newService);
        } else if (services.size() > 0) {
            newService = CloneUtil.cloneObject(services.get(0));
            newService.setValueId(null);
        }
        return newService;
    }

    public List<Attribute> getServices() {
        return services;
    }
}
