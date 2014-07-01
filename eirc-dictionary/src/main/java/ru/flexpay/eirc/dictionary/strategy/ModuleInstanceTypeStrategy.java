/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.flexpay.eirc.dictionary.strategy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.wicket.util.string.Strings;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.example.AttributeExample;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.strategy.DeleteException;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.util.ResourceUtil;
import org.complitex.template.strategy.TemplateStrategy;
import org.complitex.template.web.security.SecurityRole;

import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author Artem
 */
@Stateless
public class ModuleInstanceTypeStrategy extends TemplateStrategy {

    private static final String RESOURCE_BUNDLE = ModuleInstanceTypeStrategy.class.getName();
    /**
     * Attribute type ids
     */
    public static final long NAME = 1110;
    /**
     * Organization type ids
     */
    public static final long EIRC_TYPE = 1;
    public static final long PAYMENTS_TYPE = 2;

    @Override
    public String getEntityTable() {
        return "module_instance_type";
    }

    @Override
    protected List<Long> getListAttributeTypes() {
        return Lists.newArrayList(NAME);
    }

    @Override
    public String displayDomainObject(DomainObject object, Locale locale) {
        return AttributeUtil.getStringCultureValue(object, NAME, locale);
    }

    @Override
    public void configureExample(DomainObjectExample example, Map<String, Long> ids, String searchTextInput) {
        if (!Strings.isEmpty(searchTextInput)) {
            AttributeExample attrExample = example.getAttributeExample(NAME);
            if (attrExample == null) {
                attrExample = new AttributeExample(NAME);
                example.addAttributeExample(attrExample);
            }
            attrExample.setValue(searchTextInput);
        }
    }

    @Override
    public String getPluralEntityLabel(Locale locale) {
        return ResourceUtil.getString(RESOURCE_BUNDLE, getEntityTable(), locale);
    }

    public List<? extends DomainObject> getAll() {
        DomainObjectExample example = new DomainObjectExample();
        configureExample(example, ImmutableMap.<String, Long>of(), null);

        return find(example);
    }

    @Override
    public String[] getEditRoles() {
        return new String[]{SecurityRole.ADDRESS_MODULE_EDIT};
    }

    @Override
    public String[] getListRoles() {
        return new String[]{SecurityRole.ADDRESS_MODULE_VIEW};
    }

    protected Collection<Long> getReservedInstanceIds() {
        return ImmutableList.of(EIRC_TYPE, PAYMENTS_TYPE);
    }

    @Override
    protected void deleteChecks(long objectId, Locale locale) throws DeleteException {
        if (getReservedInstanceIds().contains(objectId)) {
            throw new DeleteException(ResourceUtil.getString(RESOURCE_BUNDLE, "delete_reserved_instance_error", locale));
        }
        super.deleteChecks(objectId, locale);
    }
}
