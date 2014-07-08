package ru.flexpay.eirc.registry.web.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.complitex.dictionary.web.component.datatable.DataProvider;
import org.odlabs.wiquery.ui.accordion.Accordion;
import org.odlabs.wiquery.ui.options.HeightStyleEnum;
import ru.flexpay.eirc.registry.entity.Container;

import java.util.List;

/**
 * @author Pavel Sknar
 */
public class ContainerListPanel<T> extends Panel {

    public ContainerListPanel(String id, IModel<T> model) {
        super(id, model);
        init();
    }

    private void init() {
        Object object = getDefaultModelObject();

        final List<Container> containers = new CompoundPropertyModel<>(object).<List<Container>>bind(getPropertyName()).getObject();

        Accordion accordion = new Accordion("accordionContainers");
        accordion.setCollapsible(true);
        accordion.setActive(false);
        accordion.setOutputMarkupPlaceholderTag(true);
        accordion.setHeightStyle(HeightStyleEnum.CONTENT);
        add(accordion);

        final DataProvider<Container> dataProvider = new DataProvider<Container>() {
            @Override
            protected Iterable<? extends Container> getData(long first, long count) {
                return containers;
            }

            @Override
            protected int getSize() {
                return containers.size();
            }
        };

        DataView<Container> dataView = new DataView<Container>("containersData", dataProvider,
                containers.size()) {
            @Override
            protected void populateItem(Item<Container> item) {
                item.add(new Label("container", item.getModelObject().getData()));
            }
        };

        accordion.add(dataView);
    }

    protected String getPropertyName() {
        return "containers";
    }


}
