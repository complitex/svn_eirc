package ru.flexpay.eirc.registry.service;

import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.ContainerType;

/**
 * @author Pavel Sknar
 */
public class ContainerProxy extends Container {
    private Container container;

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public String getData() {
        return container.getData();
    }

    @Override
    public void setType(ContainerType type) {
        container.setType(type);
    }

    @Override
    public ContainerType getType() {
        return container.getType();
    }

    @Override
    public void setData(String data) {
        container.setData(data);
    }

    @Override
    public Long getId() {
        return container.getId();
    }

    @Override
    public void setId(Long id) {
        container.setId(id);
    }
}