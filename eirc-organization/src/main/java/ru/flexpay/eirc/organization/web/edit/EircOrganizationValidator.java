package ru.flexpay.eirc.organization.web.edit;

import org.complitex.dictionary.entity.DomainObject;
import org.complitex.organization.strategy.web.edit.OrganizationEditComponent;
import org.complitex.organization.strategy.web.edit.OrganizationValidator;

import java.util.Locale;

/**
 *
 * @author Artem
 */
public class EircOrganizationValidator extends OrganizationValidator {

    public EircOrganizationValidator(Locale systemLocale) {
        super(systemLocale);
    }

    @Override
    protected boolean checkDistrict(DomainObject object, OrganizationEditComponent editComponent) {
        EircOrganizationEditComponent editComp = (EircOrganizationEditComponent) editComponent;
        if (editComp.isServiceProvider()) {
            boolean validated = editComponent.isDistrictEntered();
            if (!validated) {
                editComponent.error(editComponent.getString("must_have_district"));
            }
            return validated;
        } else {
            return true;
        }
    }
}
