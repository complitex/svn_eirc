package ru.flexpay.eirc.payments_communication.service;

import com.google.common.collect.Lists;
import ru.flexpay.eirc.payments_communication.entity.QuittanceDebtInfo;
import ru.flexpay.eirc.payments_communication.entity.QuittanceInfo;
import ru.flexpay.eirc.payments_communication.entity.ResponseStatus;
import ru.flexpay.eirc.payments_communication.entity.ServiceDetails;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

/**
 * @author Pavel Sknar
 */
@Stateless
@LocalBean
@Path("/quittanceDebtInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RolesAllowed(SecurityRole.AUTHORIZED)
public class QuittanceDebtInfoService extends RestAuthorizationService<QuittanceDebtInfo> {

    @Override
    protected QuittanceDebtInfo getAllAuthorized(String moduleUniqueIndex) {
        return buildResponseContent(ResponseStatus.OK);
    }

    @Override
    protected QuittanceDebtInfo geConstrainedAuthorized(String searchCriteria, long searchType, String moduleUniqueIndex) {
        QuittanceInfo quittanceInfo1 = new QuittanceInfo();
        quittanceInfo1.setQuittanceNumber("111");
        quittanceInfo1.setTotalToPay(BigDecimal.TEN);
        quittanceInfo1.setTotalPayed(BigDecimal.ONE);
        quittanceInfo1.setCountry("Ukrain");
        quittanceInfo1.setCreationDate(new Date());
        quittanceInfo1.setBeginDate(new Date());

        ServiceDetails serviceDetails1 = new ServiceDetails();
        serviceDetails1.setAmount(BigDecimal.ONE);
        serviceDetails1.setCountry("Ukrain");
        serviceDetails1.setCity("Kyev");
        serviceDetails1.setPayment(new BigDecimal(90d));
        serviceDetails1.setIncomingBalance(BigDecimal.ZERO);
        serviceDetails1.setOutgoingBalance(new BigDecimal(100d));

        quittanceInfo1.getServiceDetails().add(serviceDetails1);

        ServiceDetails serviceDetails2 = new ServiceDetails();
        serviceDetails2.setAmount(BigDecimal.TEN);
        serviceDetails2.setCountry("Ukrain");
        serviceDetails2.setCity("Kharkov");
        serviceDetails2.setPayment(new BigDecimal(90d));
        serviceDetails2.setIncomingBalance(BigDecimal.TEN);
        serviceDetails2.setOutgoingBalance(new BigDecimal(100d));

        quittanceInfo1.getServiceDetails().add(serviceDetails2);

        QuittanceInfo quittanceInfo2 = new QuittanceInfo();
        quittanceInfo2.setQuittanceNumber("222");
        quittanceInfo2.setTotalToPay(BigDecimal.TEN);
        quittanceInfo2.setTotalPayed(BigDecimal.ZERO);
        quittanceInfo2.setCountry("Ukrain");
        quittanceInfo2.setCity("Kharkov");
        quittanceInfo2.setCreationDate(new Date());
        quittanceInfo2.setBeginDate(new Date());

        return new QuittanceDebtInfo(ResponseStatus.OK, Lists.newArrayList(quittanceInfo1, quittanceInfo2));
    }

    @Override
    protected QuittanceDebtInfo buildResponseContent(ResponseStatus responseStatus) {
        return new QuittanceDebtInfo(responseStatus, Collections.<QuittanceInfo>emptyList());
    }
}
