package ru.flexpay.eirc.organization_type.web;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.complitex.dictionary.strategy.IStrategy;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.complitex.template.web.security.SecurityRole;
import ru.flexpay.eirc.organization_type.strategy.EircOrganizationTypeStrategy;

/**
 *
 * @author Artem
 */
@AuthorizeInstantiation(SecurityRole.ORGANIZATION_MODULE_EDIT)
public class OrganizationTypeMenu extends org.complitex.organization_type.menu.OrganizationTypeMenu {

    @Override
    protected IStrategy getStrategy() {
        return EjbBeanLocator.getBean(EircOrganizationTypeStrategy.class);
    }
}
