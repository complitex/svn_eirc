package ru.flexpay.eirc.organization.strategy;

import com.google.common.collect.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.StringCulture;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.strategy.DeleteException;
import org.complitex.dictionary.strategy.organization.IOrganizationStrategy;
import org.complitex.dictionary.strategy.web.AbstractComplexAttributesPanel;
import org.complitex.dictionary.strategy.web.validate.IValidator;
import org.complitex.dictionary.util.AttributeUtil;
import org.complitex.dictionary.util.ResourceUtil;
import org.complitex.organization.strategy.AbstractOrganizationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.organization.strategy.entity.EircOrganization;
import ru.flexpay.eirc.organization.strategy.entity.RemoteDataSource;
import ru.flexpay.eirc.organization.strategy.web.edit.EircOrganizationEditComponent;
import ru.flexpay.eirc.organization.strategy.web.edit.EircOrganizationValidator;
import ru.flexpay.eirc.organization_type.strategy.EircOrganizationTypeStrategy;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.*;
import javax.sql.DataSource;
import java.util.*;

/**
 * @author Artem
 */
@Stateless(name = IOrganizationStrategy.BEAN_NAME)
public class EircOrganizationStrategy extends AbstractOrganizationStrategy {
    public final static Long MODULE_ID = 0L;

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
     * Itself organization instance id.
     */


    private static final Logger log = LoggerFactory.getLogger(EircOrganizationStrategy.class);
    public static final String EIRC_ORGANIZATION_STRATEGY_NAME = IOrganizationStrategy.BEAN_NAME;
    private static final String RESOURCE_BUNDLE = EircOrganizationStrategy.class.getName();
    private static final String MAPPING_NAMESPACE = EircOrganizationStrategy.class.getPackage().getName() + ".EircOrganization";


    private static final List<Long> CUSTOM_ATTRIBUTE_TYPES = ImmutableList.<Long>builder().
            add(KPP).
            add(INN).
            add(NOTE).
            add(JURIDICAL_ADDRESS).
            add(POSTAL_ADDRESS).
            add(EMAIL).
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

    @Transactional
    @Override
    public List<DomainObject> getAllOuterOrganizations(Locale locale) {
        DomainObjectExample example = new DomainObjectExample();
        if (locale != null) {
            example.setOrderByAttributeTypeId(NAME);
            example.setLocaleId(localeBean.convert(locale).getId());
            example.setAsc(true);
        }
        example.addAdditionalParam(ORGANIZATION_TYPE_PARAMETER, ImmutableList.of(EircOrganizationTypeStrategy.SERVICE_PROVIDER));
        configureExample(example, ImmutableMap.<String, Long>of(), null);
        return (List<DomainObject>) find(example);
    }

    @Override
    public Long getModuleId() {
        return MODULE_ID;
    }

    /**
     * Figures out all EIRC organizations visible to current user
     * and returns them sorted by organization's name in given {@code locale}.
     *
     * @param locale Locale. It is used in sorting of organizations by name.
     * @return All EIRC organizations.
     */
    @Transactional
    public List<DomainObject> getAllServiceProviders(Locale locale) {
        DomainObjectExample example = new DomainObjectExample();

        example.addAdditionalParam(ORGANIZATION_TYPE_PARAMETER, ImmutableList.of(EircOrganizationTypeStrategy.SERVICE_PROVIDER));
        if (locale != null) {
            example.setOrderByAttributeTypeId(NAME);
            example.setLocaleId(localeBean.convert(locale).getId());
            example.setAsc(true);
        }

        configureExample(example, ImmutableMap.<String, Long>of(), null);

        return (List<DomainObject>) find(example);
    }

    @Override
    public boolean isSimpleAttributeType(EntityAttributeType entityAttributeType) {
        if (CUSTOM_ATTRIBUTE_TYPES.contains(entityAttributeType.getId())) {
            return false;
        }
        return super.isSimpleAttributeType(entityAttributeType);
    }

    @Override
    protected void fillAttributes(DomainObject object) {
        super.fillAttributes(object);

        for (long attributeTypeId : CUSTOM_ATTRIBUTE_TYPES) {
            if (object.getAttribute(attributeTypeId).getLocalizedValues() == null) {
                object.getAttribute(attributeTypeId).setLocalizedValues(stringBean.newStringCultures());
            }
        }
    }

    @Override
    protected void loadStringCultures(List<Attribute> attributes) {
        super.loadStringCultures(attributes);

        for (Attribute attribute : attributes) {
            if (CUSTOM_ATTRIBUTE_TYPES.contains(attribute.getAttributeTypeId())) {
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
    public EircOrganization findById(long id, boolean runAsAdmin) {
        DomainObject object = super.findById(id, runAsAdmin);
        if (object == null) {
            return null;
        }

        return new EircOrganization(object);
    }

    @Override
    public EircOrganization newInstance() {
        return new EircOrganization(super.newInstance());
    }

    @Override
    public EircOrganization findHistoryObject(long objectId, Date date) {
        DomainObject object = super.findHistoryObject(objectId, date);
        if (object == null) {
            return null;
        }
        return new EircOrganization(object);
    }

    @Transactional
    @Override
    public void insert(DomainObject object, Date insertDate) {
        //EircOrganization eircOrganization = (EircOrganization) object;

        super.insert(object, insertDate);
    }

    @Transactional
    @Override
    public void update(DomainObject oldObject, DomainObject newObject, Date updateDate) {
        //EircOrganization newOrganization = (EircOrganization) newObject;
        //EircOrganization oldOrganization = (EircOrganization) oldObject;

        super.update(oldObject, newObject, updateDate);
    }

    @Transactional
    @Override
    protected void deleteChecks(long objectId, Locale locale) throws DeleteException {
        if (MODULE_ID == objectId) {
            throw new DeleteException(ResourceUtil.getString(RESOURCE_BUNDLE, "delete_reserved_instance_error", locale));
        }
        super.deleteChecks(objectId, locale);
    }

    @Transactional
    @Override
    public void delete(long objectId, Locale locale) throws DeleteException {
        deleteChecks(objectId, locale);

        deleteStrings(objectId);
        deleteAttribute(objectId);
        deleteObject(objectId, locale);
    }

    /**
     * Finds remote jdbc data sources.
     *
     * @param currentDataSource Current data source.
     * @return Remote jdbc data sources.
     */
    public List<RemoteDataSource> findRemoteDataSources(String currentDataSource) {
        final String JDBC_PREFIX = "jdbc";
        final String GLASSFISH_INTERNAL_SUFFIX = "__pm";
        final Set<String> PREDEFINED_DATA_SOURCES = ImmutableSet.of("sample", "__TimerPool", "__default");

        Set<RemoteDataSource> remoteDataSources = Sets.newTreeSet(new Comparator<RemoteDataSource>() {

            @Override
            public int compare(RemoteDataSource o1, RemoteDataSource o2) {
                return o1.getDataSource().compareTo(o2.getDataSource());
            }
        });

        boolean currentDataSourceEnabled = false;

        try {
            Context context = new InitialContext();
            final NamingEnumeration<NameClassPair> resources = context.list(JDBC_PREFIX);
            if (resources != null) {
                while (resources.hasMore()) {
                    final NameClassPair nc = resources.next();
                    if (nc != null) {
                        final String name = nc.getName();
                        if (!Strings.isEmpty(name) && !name.endsWith(GLASSFISH_INTERNAL_SUFFIX)
                                && !PREDEFINED_DATA_SOURCES.contains(name)) {
                            final String fullDataSource = JDBC_PREFIX + "/" + name;
                            Object jndiObject = null;
                            try {
                                jndiObject = context.lookup(fullDataSource);
                            } catch (NamingException e) {
                            }

                            if (jndiObject instanceof DataSource) {
                                boolean current = false;
                                if (fullDataSource.equals(currentDataSource)) {
                                    currentDataSourceEnabled = true;
                                    current = true;
                                }
                                remoteDataSources.add(new RemoteDataSource(fullDataSource, current));
                            }
                        }

                    }
                }
            }
        } catch (NamingException e) {
            log.error("", e);
        }

        if (!currentDataSourceEnabled && !Strings.isEmpty(currentDataSource)) {
            remoteDataSources.add(new RemoteDataSource(currentDataSource, true, false));
        }

        return Lists.newArrayList(remoteDataSources);
    }

    /**
     * Figures out data source of calculation center.
     *
     * @param calculationCenterId Calculation center's id
     * @return Calculation center's data source
     */
    public String getDataSource(long calculationCenterId) {
        DomainObject calculationCenter = findById(calculationCenterId, true);
        return AttributeUtil.getStringValue(calculationCenter, KPP);
    }

    /**
     * Returns relative path to request files storage.
     *
     * @param eircId                  Eirc's id.
     * @param fileTypeAttributeTypeId Attribute type id corresponding desired file type.
     * @return Relative path to request files storage.
     */
    public String getRelativeRequestFilesPath(long eircId, long fileTypeAttributeTypeId) {
        DomainObject eirc = findById(eircId, true);
        return AttributeUtil.getStringValue(eirc, fileTypeAttributeTypeId);
    }

    @Transactional
    @Override
    protected Long insertStrings(long attributeTypeId, List<StringCulture> strings) {
        /* if it's data source or one of load/save request file directory attributes 
         * or root directory for loading and saving request files
         * then string value should be inserted as is and not upper cased. */
        return CUSTOM_ATTRIBUTE_TYPES.contains(attributeTypeId)
                ? stringBean.insertStrings(strings, getEntityTable(), false)
                : super.insertStrings(attributeTypeId, strings);
    }
}
