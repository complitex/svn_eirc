package ru.flexpay.eirc.payments_communication.service;

import com.google.common.collect.Lists;
import org.complitex.address.entity.AddressEntity;
import org.complitex.address.strategy.apartment.ApartmentStrategy;
import org.complitex.address.strategy.building_address.BuildingAddressStrategy;
import org.complitex.address.strategy.room.RoomStrategy;
import org.complitex.correction.entity.ApartmentCorrection;
import org.complitex.correction.entity.BuildingCorrection;
import org.complitex.correction.entity.RoomCorrection;
import org.complitex.correction.service.AddressCorrectionBean;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.Correction;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.EircConfig;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;
import ru.flexpay.eirc.eirc_account.entity.EircAccount;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.payments_communication.entity.DebInfo;
import ru.flexpay.eirc.payments_communication.entity.SearchType;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service_provider_account.entity.SaldoOut;
import ru.flexpay.eirc.service_provider_account.entity.ServiceProviderAccount;
import ru.flexpay.eirc.service_provider_account.service.SaldoOutBean;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
@LocalBean
@Path("/debInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RolesAllowed(SecurityRole.AUTHORIZED)
public class DebtInfoService extends RestAuthorizationService<DebInfo> {

    @EJB
    private SaldoOutBean saldoOutBean;

    @EJB
    private AddressCorrectionBean addressCorrectionBean;

    @EJB
    private BuildingAddressStrategy buildingAddressStrategy;

    @EJB
    private ApartmentStrategy apartmentStrategy;

    @EJB
    private RoomStrategy roomStrategy;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private ModuleInstanceStrategy moduleInstanceStrategy;

    @EJB
    private EircOrganizationStrategy organizationStrategy;

    @EJB
    private ConfigBean configBean;

    private Logger logger = LoggerFactory.getLogger(DebtInfoService.class);

    @Override
    protected List<DebInfo> getAllAuthorized() {
        return Collections.emptyList();
    }

    @Override
    protected List<DebInfo> geConstrainedAuthorized(String searchCriteria, long searchType) {
        Integer eircModuleId = configBean.getInteger(EircConfig.MODULE_ID, true);
        if (eircModuleId == null || eircModuleId < 0) {
            logger.error("Inner error: EIRC module did not configure");
            return Collections.emptyList();
        }

        DomainObject eircModule = moduleInstanceStrategy.findById(eircModuleId.longValue(), true);
        if (eircModule == null) {
            logger.error("Inner error: EIRC module instance not found by id '{}'", eircModuleId);
            return Collections.emptyList();
        }

        Attribute attribute = eircModule.getAttribute(ModuleInstanceStrategy.ORGANIZATION);
        final Long eircOrganizationId;
        if (attribute == null ||
                (eircOrganizationId = Long.parseLong(attribute.getStringCulture(EjbBeanLocator.getBean(LocaleBean.class).getSystemLocaleId()).getValue())) == null) {
            logger.error("Inner error: EIRC module '{}' did not content own organization", eircModuleId);
            return Collections.emptyList();
        }

        SearchType type = SearchType.getSearchType(searchType);

        String[] reqMas = searchCriteria.split(":");
        Service service = reqMas.length > 1 ? serviceBean.getService(Long.parseLong(reqMas[0])) : null;

        String searchString = reqMas.length > 1 ? reqMas[1] : reqMas[0];
        switch (type) {
            case TYPE_BUILDING_NUMBER:

                return getByAddressMasterIndex(searchString, service, new AddressDataProvider<BuildingCorrection>() {
                    @Override
                    protected List<BuildingCorrection> getAddressCorrection(String addressId, Long organizationId) {
                        FilterWrapper<BuildingCorrection> filter = FilterWrapper.of(
                                new BuildingCorrection(null, addressId, null, null, null, organizationId, eircOrganizationId, null)
                        );
                        return addressCorrectionBean.getBuildingCorrections(filter);
                    }

                    @Override
                    protected Address getAddress(Long id) {
                        DomainObject domainObject = buildingAddressStrategy.findById(id, true);
                        if (domainObject == null) {
                            return null;
                        }
                        return new Address(id, AddressEntity.BUILDING);
                    }
                });
            case TYPE_APARTMENT_NUMBER:
                return getByAddressMasterIndex(searchString, service, new AddressDataProvider<ApartmentCorrection>() {
                    @Override
                    protected List<ApartmentCorrection> getAddressCorrection(String addressId, Long organizationId) {
                        return addressCorrectionBean.getApartmentCorrections(null, addressId, null, null, organizationId, eircOrganizationId);
                    }

                    @Override
                    protected Address getAddress(Long id) {
                        DomainObject domainObject = apartmentStrategy.findById(id, true);
                        if (domainObject == null) {
                            return null;
                        }
                        return new Address(id, AddressEntity.APARTMENT);
                    }
                });
            case TYPE_ROOM_NUMBER:
                return getByAddressMasterIndex(searchString, service, new AddressDataProvider<RoomCorrection>() {
                    @Override
                    protected List<RoomCorrection> getAddressCorrection(String addressId, Long organizationId) {
                        return addressCorrectionBean.getRoomCorrections(null, null, addressId, null, null, organizationId, eircOrganizationId);
                    }

                    @Override
                    protected Address getAddress(Long id) {
                        DomainObject domainObject = apartmentStrategy.findById(id, true);
                        if (domainObject == null) {
                            return null;
                        }
                        return new Address(id, AddressEntity.APARTMENT);
                    }
                });
            case UNKNOWN_TYPE:
                break;
        }
        return Collections.emptyList();
    }

    private <T extends Correction> List<DebInfo> getByAddressMasterIndex(String searchString, Service service, AddressDataProvider<T> dataProvider) {
        String[] moduleData = searchString.split("-", 2);
        String addressId = moduleData.length > 1 ? moduleData[1] : moduleData[0];

        Long moduleId = null;
        if (moduleData.length > 1) {
            moduleId = moduleInstanceStrategy.getModuleInstanceObjectId(moduleData[0]);
            if (moduleId == null) {
                // TODO module not found
                return Collections.emptyList();
            }
            Integer eircModuleId = configBean.getInteger(EircConfig.MODULE_ID, true);
            if (eircModuleId != null && eircModuleId.longValue() == moduleId) {
                moduleId = null;
            } else {
                logger.warn("Own module did not configure");
            }
        }

        Long organizationId = null;
        if (moduleId != null) {
            DomainObject module = moduleInstanceStrategy.findById(moduleId, true);
            Attribute attribute = module.getAttribute(ModuleInstanceStrategy.ORGANIZATION);
            if (attribute == null) {
                // TODO organization not found
                return Collections.emptyList();
            }
            organizationId = Long.parseLong(attribute.getStringCulture(EjbBeanLocator.getBean(LocaleBean.class).getSystemLocaleId()).getValue());
            Organization organization = organizationStrategy.findById(organizationId, true);
            if (organization == null) {
                // TODO organization not found
                return Collections.emptyList();
            }
        }

        Address address = dataProvider.getAddress(addressId, organizationId);
        if (address == null) {
            // TODO address not found
            return Collections.emptyList();
        }
        SaldoOut filterObject = new SaldoOut();
        filterObject.setServiceProviderAccount(new ServiceProviderAccount(new EircAccount(address), service));
        FilterWrapper<SaldoOut> filterWrapper = FilterWrapper.of(filterObject);

        List<SaldoOut> saldoOuts = saldoOutBean.getFinancialAttributes(filterWrapper, true);
        if (saldoOuts.isEmpty()) {
            return Collections.emptyList();
        }
        List<DebInfo> result = Lists.newArrayList();
        for (SaldoOut saldoOut : saldoOuts) {
            ServiceProviderAccount serviceProviderAccount = saldoOut.getServiceProviderAccount();
            service = serviceProviderAccount.getService();

            DebInfo debInfo = new DebInfo();

            debInfo.setAmount(saldoOut.getAmount());
            debInfo.setServiceCode(service.getCode());
            debInfo.setServiceId(service.getId());
            debInfo.setServiceName(service.getName());
            debInfo.setEircAccount(serviceProviderAccount.getEircAccount().getAccountNumber());
            debInfo.setServiceProviderAccount(serviceProviderAccount.getAccountNumber());

            result.add(debInfo);
        }
        return result;
    }

    private abstract class AddressDataProvider<T extends Correction> {
        public Address getAddress(String externalId, Long organizationId) {
            Long internalId = null;
            if (organizationId != null) {
                List<T> corrections = getAddressCorrection(externalId, organizationId);
                if (corrections.size() > 1) {
                    // TODO several corrections on one address
                    return null;
                } else if (corrections.size() == 0) {
                    // TODO correction not found
                    return null;
                }
                internalId = corrections.get(0).getObjectId();
            } else {
                internalId = Long.parseLong(externalId);
            }
            return getAddress(internalId);
        }

        protected abstract List<T> getAddressCorrection(String addressId, Long organizationId);

        protected abstract Address getAddress(Long id);
    }
}
