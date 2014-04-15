package ru.flexpay.eirc.dictionary.strategy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.string.Strings;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.StringCulture;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.entity.example.AttributeExample;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.web.AbstractComplexAttributesPanel;
import org.complitex.dictionary.strategy.web.DomainObjectListPanel;
import org.complitex.dictionary.util.ResourceUtil;
import org.complitex.dictionary.web.component.search.ISearchCallback;
import org.complitex.template.strategy.TemplateStrategy;
import org.complitex.template.web.pages.DomainObjectEdit;
import ru.flexpay.eirc.dictionary.strategy.resource.EircResources;
import ru.flexpay.eirc.dictionary.web.ModuleInstancePrivateKeyPanel;
import ru.flexpay.eirc.dictionary.web.security.SecurityRole;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ModuleInstanceStrategy extends TemplateStrategy {

    @EJB
    private StringCultureBean stringBean;

    /*
     * Attribute type ids
     */
    public static final long NAME = 1010L;
    public static final long PRIVATE_KEY = 1011L;
    public static final long UNIQUE_INDEX = 1012L;

    private static final String MODULE_INSTANCE_NAMESPACE = ModuleInstanceStrategy.class.getPackage().getName() + ".ModuleInstance";

    @Override
    protected List<Long> getListAttributeTypes() {
        return Lists.newArrayList(NAME, UNIQUE_INDEX);
    }

    @Override
    public String getEntityTable() {
        return "module_instance";
    }

    @Override
    public String displayDomainObject(DomainObject object, Locale locale) {
        return stringBean.displayValue(object.getAttribute(NAME).getLocalizedValues(), locale);
    }

    @Override
    public ISearchCallback getSearchCallback() {
        return new SearchCallback();
    }

    private void configureExampleImpl(DomainObjectExample example, Map<String, Long> ids, String searchTextInput) {
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
    public void configureExample(DomainObjectExample example, Map<String, Long> ids, String searchTextInput) {
        configureExampleImpl(example, ids, searchTextInput);
    }

    @Override
    public String[] getEditRoles() {
        return new String[]{SecurityRole.MODULE_INSTANCE_VIEW, SecurityRole.MODULE_INSTANCE_EDIT};
    }

    private class SearchCallback implements ISearchCallback, Serializable {

        @Override
        public void found(Component component, Map<String, Long> ids, AjaxRequestTarget target) {
            DomainObjectListPanel list = component.findParent(DomainObjectListPanel.class);
            configureExampleImpl(list.getExample(), ids, null);
            list.refreshContent(target);
        }
    }

    @Override
    public String getPluralEntityLabel(Locale locale) {
        return ResourceUtil.getString(EircResources.class.getName(), getEntityTable(), locale);
    }

    @Override
    public boolean allowProceedNextSearchFilter() {
        return true;
    }

    @Override
    public String[] getListRoles() {
        return new String[]{SecurityRole.MODULE_INSTANCE_VIEW};
    }

    @Override
    public Class<? extends WebPage> getEditPage() {
        return DomainObjectEdit.class;
    }

    @Override
    protected void extendOrderBy(DomainObjectExample example) {
        if (example.getOrderByAttributeTypeId() != null
                && example.getOrderByAttributeTypeId().equals(NAME)) {
            example.setOrderByNumber(true);
        }
    }

    /**
     * Найти квартиру в локальной адресной базе.
     */
    public List<Long> getModuleInstanceObjectIds(String name) {
        Map<String, Object> params = Maps.newHashMap();

        params.put("name", name);

        return sqlSession().selectList(MODULE_INSTANCE_NAMESPACE + ".selectModuleInstances", params);
    }

    @Override
    public boolean isSimpleAttributeType(EntityAttributeType entityAttributeType) {
        return entityAttributeType.getId() != PRIVATE_KEY && super.isSimpleAttributeType(entityAttributeType);
    }

    @Override
    protected void fillAttributes(DomainObject object) {
        super.fillAttributes(object);

        if (object.getAttribute(PRIVATE_KEY).getLocalizedValues() == null) {
            object.getAttribute(PRIVATE_KEY).setLocalizedValues(stringBean.newStringCultures());
        }
    }

    @Override
    protected void loadStringCultures(List<Attribute> attributes) {
        super.loadStringCultures(attributes);

        for (Attribute attribute : attributes) {
            if (attribute.getAttributeTypeId() == PRIVATE_KEY) {
                if (attribute.getValueId() != null) {
                    loadStringCultures(attribute);
                } else {
                    attribute.setLocalizedValues(stringBean.newStringCultures());
                }
            }
        }
    }

    @Transactional
    @Override
    protected Long insertStrings(long attributeTypeId, List<StringCulture> strings) {
        /* if it's data source or one of load/save request file directory attributes
         * or root directory for loading and saving request files
         * then string value should be inserted as is and not upper cased. */
        return attributeTypeId == PRIVATE_KEY
                ? stringBean.insertStrings(strings, getEntityTable(), false)
                : super.insertStrings(attributeTypeId, strings);
    }

    @Override
    public Class<? extends AbstractComplexAttributesPanel> getComplexAttributesPanelAfterClass() {
        return ModuleInstancePrivateKeyPanel.class;
    }
}
