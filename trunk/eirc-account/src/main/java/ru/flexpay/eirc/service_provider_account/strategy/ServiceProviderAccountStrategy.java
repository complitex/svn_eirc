package ru.flexpay.eirc.service_provider_account.strategy;

import org.apache.wicket.util.string.Strings;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.SimpleTypes;
import org.complitex.dictionary.entity.StringCulture;
import org.complitex.dictionary.entity.description.EntityAttributeType;
import org.complitex.dictionary.entity.example.DomainObjectExample;
import org.complitex.dictionary.service.StringCultureBean;
import org.complitex.dictionary.util.Numbers;
import org.complitex.dictionary.util.StringUtil;
import org.complitex.template.strategy.TemplateStrategy;
import org.complitex.template.web.security.SecurityRole;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Sknar
 */
@Stateless
public class ServiceProviderAccountStrategy extends TemplateStrategy {

    /**
     * Attribute ids
     */
    // Количество проживающих
    public static final long NUMBER_OF_HABITANTS = 6001L;
    // Площадь общая
    public static final long TOTAL_SQUARE = 6002L;
    // Площадь жилая
    public static final long LIVE_SQUARE = 6003L;
    // Площадь отапливаемая
    public static final long HEATED_SQUARE = 6004L;
    // Площадь отапливаемая
    public static final long RESPONSIBLE_PERSON = 6005L;

    @EJB
    private StringCultureBean stringBean;

    @Override
    public Long getObjectId(String externalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends DomainObject> find(DomainObjectExample example) {
        throw new UnsupportedOperationException("Use findById");
    }

    @Override
    public DomainObject findById(Long id, boolean runAsAdmin) {

        DomainObject object = new DomainObject(id);

        loadAttributes(object);
        fillAttributes(object);
        updateStringsForNewLocales(object);

        return object;
    }

    @Override
    public int count(DomainObjectExample example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(DomainObject object, Date insertDate) {
        for (Attribute attribute : object.getAttributes()) {
            attribute.setObjectId(object.getId());
            attribute.setStartDate(insertDate);
            insertAttribute(attribute);
        }
    }

    @Override
    public void update(DomainObject oldObject, DomainObject newObject, Date updateDate) {

        //attributes comparison
        for (Attribute oldAttr : oldObject.getAttributes()) {
            boolean removed = true;
            for (Attribute newAttr : newObject.getAttributes()) {
                if (oldAttr.getAttributeTypeId().equals(newAttr.getAttributeTypeId()) && oldAttr.getAttributeId().equals(newAttr.getAttributeId())) {
                    //the same attribute_type and the same attribute_id
                    removed = false;
                    boolean needToUpdateAttribute = false;

                    EntityAttributeType attributeType = getEntity().getAttributeType(oldAttr.getAttributeTypeId());

                    Long oldValueTypeId = oldAttr.getValueTypeId();
                    Long newValueTypeId = newAttr.getValueTypeId();

                    if (!Numbers.isEqual(oldValueTypeId, newValueTypeId)) {
                        needToUpdateAttribute = true;
                    } else {
                        String attributeValueType = attributeType.getAttributeValueType(oldAttr.getValueTypeId()).getValueType();
                        if (SimpleTypes.isSimpleType(attributeValueType)) {
                            SimpleTypes simpleType = SimpleTypes.valueOf(attributeValueType.toUpperCase());
                            switch (simpleType) {
                                case STRING_CULTURE: {
                                    boolean valueChanged = false;
                                    for (StringCulture oldString : oldAttr.getLocalizedValues()) {
                                        for (StringCulture newString : newAttr.getLocalizedValues()) {
                                            //compare strings
                                            if (oldString.getLocaleId().equals(newString.getLocaleId())) {
                                                if (!Strings.isEqual(oldString.getValue(), newString.getValue())) {
                                                    valueChanged = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    if (valueChanged) {
                                        needToUpdateAttribute = true;
                                    }
                                }
                                break;

                                case GENDER:
                                case BOOLEAN:
                                case DATE:
                                case DATE2:
                                case MASKED_DATE:
                                case DOUBLE:
                                case INTEGER: {
                                    String oldString = stringBean.getSystemStringCulture(oldAttr.getLocalizedValues()).getValue();
                                    String newString = stringBean.getSystemStringCulture(newAttr.getLocalizedValues()).getValue();
                                    if (!StringUtil.isEqualIgnoreCase(oldString, newString)) {
                                        needToUpdateAttribute = true;
                                    }
                                }
                                break;

                                case BIG_STRING:
                                case STRING: {
                                    String oldString = stringBean.getSystemStringCulture(oldAttr.getLocalizedValues()).getValue();
                                    String newString = stringBean.getSystemStringCulture(newAttr.getLocalizedValues()).getValue();
                                    if (!Strings.isEqual(oldString, newString)) {
                                        needToUpdateAttribute = true;
                                    }
                                }
                                break;
                            }
                        } else {
                            //reference object ids
                            Long oldValueId = oldAttr.getValueId();
                            Long newValueId = newAttr.getValueId();
                            if (!Numbers.isEqual(oldValueId, newValueId)) {
                                needToUpdateAttribute = true;
                            }
                        }
                    }

                    if (needToUpdateAttribute) {
                        archiveAttribute(oldAttr, updateDate);
                        newAttr.setStartDate(updateDate);
                        newAttr.setObjectId(newObject.getId());
                        insertAttribute(newAttr);
                    }
                }
            }
            if (removed) {
                archiveAttribute(oldAttr, updateDate);
            }
        }

        for (Attribute newAttr : newObject.getAttributes()) {
            boolean added = true;
            for (Attribute oldAttr : oldObject.getAttributes()) {
                if (oldAttr.getAttributeTypeId().equals(newAttr.getAttributeTypeId()) && oldAttr.getAttributeId().equals(newAttr.getAttributeId())) {
                    //the same attribute_type and the same attribute_id
                    added = false;
                    break;
                }
            }

            if (added) {
                newAttr.setStartDate(updateDate);
                newAttr.setObjectId(newObject.getId());
                insertAttribute(newAttr);
            }
        }
    }

    @Override
    public String displayDomainObject(DomainObject object, Locale locale) {
        return "";
    }

    @Override
    public String getEntityTable() {
        return "service_provider_account";
    }

    @Override
    public String[] getEditRoles() {
        return new String[]{SecurityRole.AUTHORIZED};
    }
}
