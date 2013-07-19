/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.web.template;

import org.apache.wicket.RestartResponseException;
import org.complitex.template.web.pages.welcome.WelcomePage;
import org.complitex.template.web.security.SecurityRole;

/**
 *
 * @author Artem
 */
public final class EircWelcomePage extends WelcomePage {

    public EircWelcomePage() {
        /*
        if (!hasAnyRole(SecurityRole.INFO_PANEL_ALLOWED)) {
            throw new RestartResponseException(ApartmentCardSearch.class);
        }*/
    }
}
