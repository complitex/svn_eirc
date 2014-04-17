package ru.flexpay.eirc.payments_communication.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.entity.Attribute;
import org.complitex.dictionary.entity.DomainObject;
import org.complitex.dictionary.service.LocaleBean;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.complitex.template.web.security.SecurityRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.strategy.ModuleInstanceStrategy;

import javax.annotation.Priority;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

/**
 * @author Pavel Sknar
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class RestHMACAuthorizationFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RestHMACAuthorizationFilter.class);

    private static final String HEADER_TIME = "LOCAL-TIME";
    private static final String HEADER_AUTH = "MODULE-AUTH";

    public RestHMACAuthorizationFilter() {
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        try {
            // validate data
            String time = requestContext.getHeaderString(HEADER_TIME);
            if (StringUtils.isEmpty(time)) {
                return;
            }
            String moduleAuth = requestContext.getHeaderString(HEADER_AUTH);
            if (StringUtils.isEmpty(moduleAuth)) {
                return;
            }
            String[] split = moduleAuth.split(":", 2);
            if (split.length != 2) {
                return;
            }
            final String moduleIndex = split[0];
            String sentSignature = split[1];

            // check authorization
            if (!checkAuth(moduleIndex, sentSignature, requestContext)) {
                return;
            }

            requestContext.setSecurityContext(
                    new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            return new UserPrincipal() {
                                @Override
                                public String getName() {
                                    return moduleIndex;
                                }
                            };
                        }

                        @Override
                        public boolean isUserInRole(String role) {
                            return StringUtils.equals(SecurityRole.AUTHORIZED, role);
                        }

                        @Override
                        public boolean isSecure() {
                            return requestContext.getSecurityContext().isSecure();
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return requestContext.getSecurityContext().getAuthenticationScheme();
                        }
                    }
            );
        } catch (Exception ex) {
            log.error("Can not authorize", ex);
        } finally {
            if (requestContext.getSecurityContext() == null || requestContext.getSecurityContext().getUserPrincipal() == null) {
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity("Login required.").build());
            }
        }
        /*
        requestContext.getSession().setAttribute(SecurityWebListener.LOGGED_IN, moduleIndex);
        */
    }

    private boolean checkAuth(String moduleIndex, String sentSignature, ContainerRequestContext requestContext) {

        Long moduleId = EjbBeanLocator.getBean(ModuleInstanceStrategy.class).getModuleInstanceObjectId(moduleIndex);
        if (moduleId == null) {
            log.warn("Module Id not found by unique module index '{}'", moduleIndex);
            return false;
        }

        DomainObject module = EjbBeanLocator.getBean(ModuleInstanceStrategy.class).findById(moduleId, true);
        if (module == null) {
            log.error("Module not found by id {}", moduleId);
            return false;
        }

        Attribute attribute = module.getAttribute(ModuleInstanceStrategy.PRIVATE_KEY);
        if (attribute == null) {
            log.warn("Module '{}' have not private key attribute '{}'", moduleIndex, ModuleInstanceStrategy.PRIVATE_KEY);
            return false;
        }

        String secretKey = attribute.getStringCulture(EjbBeanLocator.getBean(LocaleBean.class).getSystemLocaleId()).getValue();
        if (StringUtils.isEmpty(secretKey)) {
            log.warn("Module '{}' have empty private key");
            return false;
        }

        String generatedSignature = createRequestSignature(requestContext, secretKey);

        return StringUtils.equals(sentSignature, generatedSignature);
    }

    public static String createRequestSignature(final ContainerRequestContext requestContext, final String secretKey) {
        try {
            String signatureBase = createSignatureBase(requestContext);

            SecretKeySpec keySpec = new SecretKeySpec(Base64.decodeBase64(secretKey.getBytes(Charset.forName("UTF-8"))), "HmacMD5");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(keySpec);
            byte[] result = mac.doFinal(signatureBase.getBytes("UTF-8"));

            return new String(Base64.encodeBase64(result), Charset.forName("UTF-8"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String createSignatureBase(final ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> params = requestContext.getUriInfo().getPathParameters();
        String searchType = params.getFirst("searchType");
        String searchCriteria = params.getFirst("searchCriteria");

        return createSignatureBase(
                requestContext.getMethod(),
                requestContext.getHeaderString(HEADER_TIME),
                searchType + ":" + searchCriteria
        );
    }

    public static String createSignatureBase(final String method, final String dateHeader,
                                             final String base) throws IOException {
        final StringBuilder builder = new StringBuilder();

        builder.append(method).append("\n");
        builder.append(dateHeader).append("\n");
        builder.append(toMd5(base));

        return builder.toString();
    }

    private static String toMd5(String base) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return Hex.encodeHexString(md.digest(base.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
