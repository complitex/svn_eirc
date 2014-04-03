package ru.flexpay.eirc.payments_communication.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.security.SecurityWebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class RestAuthorizationService<T> {

    private static final Logger logger = LoggerFactory.getLogger(RestAuthorizationService.class);

    @GET
    @Path("/all")
    public final List<T> getAll(@Context HttpServletRequest req) throws ServletException {
        if (!auth(req)) {
            return Collections.emptyList();
        }
        return getAllAuthorized();
    }

    protected abstract List<T> getAllAuthorized();

    @GET
    @Path("/constrain/{searchType: [0-9]+}/{searchCriteria}")
    public List<T> getConstrained(@Context HttpServletRequest req,
                                    @PathParam("searchCriteria") String searchCriteria,
                                    @PathParam("searchType") int searchType) throws ServletException {

        if (!auth(req)) {
            return Collections.emptyList();
        }

        return geConstrainedAuthorized();
    }

    protected abstract List<T> geConstrainedAuthorized();

    protected boolean auth(HttpServletRequest req) throws ServletException {
        String base64Credentials = req.getHeader("Authorization");
        if (base64Credentials == null) {
            logger.error("Can not find header 'Authorization' in request: {}", req.getRequestedSessionId());
            return false;
        }
        base64Credentials = StringUtils.removeStartIgnoreCase(base64Credentials, "BASIC ");
        String credentials = new String(Base64.decodeBase64(base64Credentials));
        String[] auth = credentials.split(":");

        req.login(auth[0], auth[1]);
        req.getSession().setAttribute(SecurityWebListener.LOGGED_IN, auth[0]);

        if (!req.isUserInRole(SecurityRole.AUTHORIZED)) {
            logger.error("User '{}' have not '{}' role", auth[0], SecurityRole.AUTHORIZED);
            return false;
        }
        return true;
    }

}
