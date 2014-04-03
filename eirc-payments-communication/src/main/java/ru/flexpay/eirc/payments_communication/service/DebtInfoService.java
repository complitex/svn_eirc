package ru.flexpay.eirc.payments_communication.service;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.payments_communication.entity.DebInfo;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
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
//@RolesAllowed(SecurityRole.AUTHORIZED) Todo Wait RestAuthorizationFilter
public class DebtInfoService extends RestAuthorizationService<DebInfo> {

    private Logger logger = LoggerFactory.getLogger(DebtInfoService.class);

    @Override
    protected List<DebInfo> getAllAuthorized() {
        return Collections.emptyList();
    }

    @Override
    protected List<DebInfo> geConstrainedAuthorized() {

        DebInfo debInfo = new DebInfo();
        //req.getHeader()
        debInfo.setAmount(BigDecimal.TEN);
        debInfo.setCountry("Ukrain");
        debInfo.setPayment(new BigDecimal(90d));
        debInfo.setIncomingBalance(new BigDecimal(10d));
        debInfo.setOutgoingBalance(new BigDecimal(100d));
        return Lists.newArrayList(debInfo);
    }
}
