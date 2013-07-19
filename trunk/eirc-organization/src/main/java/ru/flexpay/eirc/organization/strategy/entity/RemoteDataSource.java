/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.organization.strategy.entity;

import java.io.Serializable;

/**
 *
 * @author Artem
 */
public class RemoteDataSource implements Serializable {

    private String dataSource;
    private boolean current;
    private boolean exist = true;

    public RemoteDataSource(String dataSource, boolean current) {
        this.dataSource = dataSource;
        this.current = current;
    }

    public RemoteDataSource(String dataSource, boolean current, boolean exist) {
        this.dataSource = dataSource;
        this.current = current;
        this.exist = exist;
    }

    public boolean isCurrent() {
        return current;
    }

    public String getDataSource() {
        return dataSource;
    }

    public boolean isExist() {
        return exist;
    }
}
