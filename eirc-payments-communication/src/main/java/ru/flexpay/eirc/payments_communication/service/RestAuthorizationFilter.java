package ru.flexpay.eirc.payments_communication.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.complitex.template.web.security.SecurityWebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

/**
 * @author Pavel Sknar
 */
// TODO since glassfish 4.0.1. Wait release.
//@Provider
//@Priority(Priorities.AUTHENTICATION)
public class RestAuthorizationFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RestAuthorizationFilter.class);

//    @Context
    HttpServletRequest httpServletRequest;

/*
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
*/
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        logger.info("{}", requestContext);
        HttpServletRequest req = httpServletRequest;
        String base64Credentials = req.getHeader("Authorization");
        if (base64Credentials == null) {
            logger.error("Can not find header 'Authorization' in request: {}", req.getRequestedSessionId());
        }
        base64Credentials = StringUtils.removeStartIgnoreCase(base64Credentials, "BASIC ");
        String credentials = new String(Base64.decodeBase64(base64Credentials));
        String[] auth = credentials.split(":");

        try {
            req.login(auth[0], auth[1]);
        } catch (ServletException e) {
            throw new IOException(e);
        }
        req.getSession().setAttribute(SecurityWebListener.LOGGED_IN, auth[0]);
    }
}
