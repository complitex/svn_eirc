package ru.flexpay.eirc.organization.strategy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.StringCulture;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.mybatis.SqlSessionFactoryBean;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.DeleteException;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.strategy.web.AbstractComplexAttributesPanel;
import org.complitex.dictionary.strategy.web.validate.IValidator;
import org.complitex.organization.strategy.AbstractOrganizationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.OrganizationType;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.web.edit.EircOrganizationEditComponent;
import ru.flexpay.eirc.organization.web.edit.EircOrganizationValidator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Artem
 */
@Stateless(name = IOrganizationStrategy.BEAN_NAME)
public class EircOrganizationStrategy extends AbstractOrganizationStrategy<DomainObject> {
    /**
     * KPP. It is EIRC only attribute.
     */
    public final static long KPP = 913;

    /**
     * INN. It is EIRC only attribute.
     */
    public final static long INN = 914;

    /**
     * Note. It is EIRC only attribute.
     */
    public final static long NOTE = 915;

    /**
     * Juridical address. It is EIRC only attribute.
     */
    public final static long JURIDICAL_ADDRESS = 916;

    /**
     * Postal address. It is EIRC only attribute.
     */
    public final static long POSTAL_ADDRESS = 917;

    /**
     * E-mail. It is EIRC only attribute.
     */
    public final static long EMAIL = 918;

    /**
     * Service. It is EIRC only attribute.
     */
    public final static long SERVICE = 919;


    /**
     * Itself organization instance id.
     */


    private final Logger log = LoggerFactory.getLogger(EircOrganizationStrategy.class);
    public static final String EIRC_ORGANIZATION_STRATEGY_NAME = IOrganizationStrategy.BEAN_NAME;
    private static final String RESOURCE_BUNDLE = EircOrganizationStrategy.class.getName();

    public static final Map<Long, String> GENERAL_ATTRIBUTE_TYPES = ImmutableMap.<Long, String>builder().
            put(KPP, "kpp").
            put(INN, "inn").
            put(NOTE, "note").
            put(JURIDICAL_ADDRESS, "juridicalAddress").
            put(POSTAL_ADDRESS, "postalAddress").
            build();

    private static final List<Long> CUSTOM_ATTRIBUTE_TYPES = ImmutableList.<Long>builder().
            add(EMAIL).
            build();

    private static final List<Long> ALL_ATTRIBUTE_TYPES = ImmutableList.<Long>builder().
            addAll(GENERAL_ATTRIBUTE_TYPES.keySet()).
            addAll(CUSTOM_ATTRIBUTE_TYPES).
            build();

    @EJB
    private LocaleBean localeBean;

    @EJB
    private StringCultureBean stringBean;

    @Override
    public IValidator getValidator() {
        return new EircOrganizationValidator(localeBean.getSystemLocale());
    }

    @Override
    public Class<? extends AbstractComplexAttributesPanel> getComplexAttributesPanelAfterClass() {
        return EircOrganizationEditComponent.class;
    }

    @Override
    public PageParameters getEditPageParams(Long objectId, Long parentId, String parentEntity) {
        PageParameters pageParameters = super.getEditPageParams(objectId, parentId, parentEntity);
        pageParameters.set(STRATEGY, EIRC_ORGANIZATION_STRATEGY_NAME);
        return pageParameters;
    }

    @Override
    public PageParameters getHistoryPageParams(long objectId) {
        PageParameters pageParameters = super.getHistoryPageParams(objectId);
        pageParameters.set(STRATEGY, EIRC_ORGANIZATION_STRATEGY_NAME);
        return pageParameters;
    }

    @Override
    public PageParameters getListPageParams() {
        PageParameters pageParameters = super.getListPageParams();
        pageParameters.set(STRATEGY, EIRC_ORGANIZATION_STRATEGY_NAME);
        return pageParameters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DomainObject> getAllOuterOrganizations(Locale locale) {
        DomainObjectExample example = new DomainObjectExample();
        if (locale != null) {
            example.setOrderByAttributeTypeId(NAME);
            example.setLocaleId(localeBean.convert(locale).getId());
            example.setAsc(true);
        }
//        example.addAdditionalParam(ORGANIZATION_TYPE_PARAMETER, ImmutableList.of(OrganizationType.SERVICE_PROVIDER.getId()));
        configureExample(example, ImmutableMap.<String, Long>of(), null);
        return find(example);
    }

    /**
     * Figures out all EIRC organizations visible to current user
     * and returns them sorted by organization's name in given {@code locale}.
     *
     * @param locale Locale. It is used in sorting of organizations by name.
     * @return All EIRC organizations.
     */
    @SuppressWarnings("unchecked")
    public List<DomainObject> getAllServiceProviders(Locale locale) {
        DomainObjectExample example = new DomainObjectExample();

        example.addAdditionalParam(ORGANIZATION_TYPE_PARAMETER, ImmutableList.of(OrganizationType.SERVICE_PROVIDER.getId()));
        if (locale != null) {
            example.setOrderByAttributeTypeId(NAME);
            example.setLocaleId(localeBean.convert(locale).getId());
            example.setAsc(true);
        }

        configureExample(example, ImmutableMap.<String, Long>of(), null);

        return find(example);
    }

    /**
     * Figures out all EIRC organizations visible to current user
     * and returns them sorted by organization's name in given {@code locale}.
     *
     * @param locale Locale. It is used in sorting of organizations by name.
     * @return All EIRC organizations.
     */
    @SuppressWarnings("unchecked")
    public List<DomainObject> getAllPaymentCollectors(Locale locale) {
        DomainObjectExample example = new DomainObjectExample();

        example.addAdditionalParam(ORGANIZATION_TYPE_PARAMETER, ImmutableList.of(OrganizationType.PAYMENT_COLLECTOR.getId()));
        if (locale != null) {
            example.setOrderByAttributeTypeId(NAME);
            example.setLocaleId(localeBean.convert(locale).getId());
            example.setAsc(true);
        }

        configureExample(example, ImmutableMap.<String, Long>of(), null);

        return find(example);
    }

    @Override
    public boolean isSimpleAttributeType(EntityAttributeType entityAttributeType) {
        if (ALL_ATTRIBUTE_TYPES.contains(entityAttributeType.getId())) {
            return false;
        }
        return super.isSimpleAttributeType(entityAttributeType);
    }

    @Override
    protected void fillAttributes(String dataSource, DomainObject object) {
        super.fillAttributes(dataSource, object);

        for (long attributeTypeId : ALL_ATTRIBUTE_TYPES) {
            if (object.getAttribute(attributeTypeId).getLocalizedValues() == null) {
                object.getAttribute(attributeTypeId).setLocalizedValues(stringBean.newStringCultures());
            }
        }
    }

    @Override
    protected void loadStringCultures(List<Attribute> attributes) {
        super.loadStringCultures(attributes);

        for (Attribute attribute : attributes) {
            if (ALL_ATTRIBUTE_TYPES.contains(attribute.getAttributeTypeId())) {
                if (attribute.getValueId() != null) {
                    loadStringCultures(attribute);
                } else {
                    attribute.setLocalizedValues(stringBean.newStringCultures());
                }
            }
        }
    }

    @Override
    public Organization findById(Long id, boolean runAsAdmin) {
        return findById(null, id, runAsAdmin);
    }

    @Override
    public Organization findById(String dataSource, Long id, boolean runAsAdmin) {
        /*if (log.isDebugEnabled()) {
            log.debug("Session manager: id={}",
                    getSqlSessionManager().getConfiguration().getEnvironment().getId());
            try {
                log.debug("Session manager: URL={}",
                        getSqlSessionManager().getConfiguration().getEnvironment().getDataSource().getConnection().getMetaData().getURL());
            } catch (Exception e) {
                //
            }
        }*/
        DomainObject object = super.findById(dataSource, id, runAsAdmin);
        if (object == null) {
            return null;
        }

        return new Organization(object);
    }

    @Override
    public Organization newInstance() {
        return new Organization(super.newInstance());
    }

    @Override
    public Organization findHistoryObject(long objectId, Date date) {
        DomainObject object = super.findHistoryObject(objectId, date);
        if (object == null) {
            return null;
        }
        return new Organization(object);
    }

    @Override
    public void insert(DomainObject object, Date insertDate) {
        //EircOrganization eircOrganization = (EircOrganization) object;

        super.insert(object, insertDate);
    }

    @Override
    public void update(DomainObject oldObject, DomainObject newObject, Date updateDate) {
        //EircOrganization newOrganization = (EircOrganization) newObject;
        //EircOrganization oldOrganization = (EircOrganization) oldObject;

        super.update(oldObject, newObject, updateDate);
    }

    @Override
    public void delete(long objectId, Locale locale) throws DeleteException {
        deleteChecks(objectId, locale);

        deleteStrings(objectId);
        deleteAttribute(objectId);
        deleteObject(objectId, locale);
    }

    @Override
    protected Long insertStrings(long attributeTypeId, List<StringCulture> strings) {
        /* if it's data source or one of load/save request file directory attributes 
         * or root directory for loading and saving request files
         * then string value should be inserted as is and not upper cased. */
        return ALL_ATTRIBUTE_TYPES.contains(attributeTypeId)
                ? stringBean.insertStrings(strings, getEntityTable(), false)
                : super.insertStrings(attributeTypeId, strings);
    }

    @Override
    public String displayAttribute(Attribute attribute, Locale locale) {
        if (attribute.getAttributeTypeId().equals(USER_ORGANIZATION_PARENT)){
            return displayShortNameAndCode(attribute.getValueId(), locale);
        }

        return super.displayAttribute(attribute, locale);
    }

    @Override
    public void setSqlSessionFactoryBean(SqlSessionFactoryBean sqlSessionFactoryBean) {
        super.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        localeBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
        stringBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
    }
}
