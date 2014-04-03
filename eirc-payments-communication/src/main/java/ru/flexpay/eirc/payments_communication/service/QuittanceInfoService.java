package ru.flexpay.eirc.payments_communication.service;

import com.google.common.collect.Lists;
import ru.flexpay.eirc.payments_communication.entity.DebInfo;
import ru.flexpay.eirc.payments_communication.entity.QuittanceInfo;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
@Stateless
@LocalBean
@Path("/quittanceInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RolesAllowed(SecurityRole.AUTHORIZED) Todo Wait RestAuthorizationFilter
public class QuittanceInfoService extends RestAuthorizationService<QuittanceInfo> {

    @Override
    protected List<QuittanceInfo> getAllAuthorized() {
        return Collections.emptyList();
    }

    @Override
    protected List<QuittanceInfo> geConstrainedAuthorized() {
        QuittanceInfo quittanceInfo1 = new QuittanceInfo();
        quittanceInfo1.setQuittanceNumber("111");
        quittanceInfo1.setTotalToPay(BigDecimal.TEN);
        quittanceInfo1.setTotalPayed(BigDecimal.ONE);
        quittanceInfo1.setCountry("Ukrain");
        quittanceInfo1.setCreationDate(new Date());
        quittanceInfo1.setBeginDate(new Date());

        DebInfo debInfo1 = new DebInfo();
        debInfo1.setAmount(BigDecimal.ONE);
        debInfo1.setCountry("Ukrain");
        debInfo1.setCity("Kyev");
        debInfo1.setPayment(new BigDecimal(90d));
        debInfo1.setIncomingBalance(BigDecimal.ZERO);
        debInfo1.setOutgoingBalance(new BigDecimal(100d));

        quittanceInfo1.getServiceDetails().add(debInfo1);

        DebInfo debInfo2 = new DebInfo();
        debInfo2.setAmount(BigDecimal.TEN);
        debInfo2.setCountry("Ukrain");
        debInfo2.setCity("Kharkov");
        debInfo2.setPayment(new BigDecimal(90d));
        debInfo2.setIncomingBalance(BigDecimal.TEN);
        debInfo2.setOutgoingBalance(new BigDecimal(100d));

        quittanceInfo1.getServiceDetails().add(debInfo2);

        QuittanceInfo quittanceInfo2 = new QuittanceInfo();
        quittanceInfo2.setQuittanceNumber("222");
        quittanceInfo2.setTotalToPay(BigDecimal.TEN);
        quittanceInfo2.setTotalPayed(BigDecimal.ZERO);
        quittanceInfo2.setCountry("Ukrain");
        quittanceInfo2.setCity("Kharkov");
        quittanceInfo2.setCreationDate(new Date());
        quittanceInfo2.setBeginDate(new Date());

        return Lists.newArrayList(quittanceInfo1, quittanceInfo2);
    }
}
