/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.web;

import org.complitex.template.web.ComplitexWebApplication;
import org.complitex.template.web.component.toolbar.ToolbarButton;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Artem
 */
public class EircWebApplication extends ComplitexWebApplication {

    @Override
    public List<? extends ToolbarButton> getApplicationToolbarButtons(String id) {
        return Collections.emptyList();//Collections.unmodifiableList(ImmutableList.of(new PspSearchButton(id)));
    }
}
