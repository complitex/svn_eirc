package ru.flexpay.eirc.registry.service.parse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
* @author Pavel Sknar
*/
class RegistryLogger implements InvocationHandler {

    private Logger logger;
    private String incMessage;

    RegistryLogger(Logger logger, Long registryId) {
        this.logger = logger;
        this.incMessage = registryId > 0? "(Registry : " + registryId + ")" : "";
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        if (
                StringUtils.equals(method.getName(), "trace") ||
                StringUtils.equals(method.getName(), "debug") ||
                StringUtils.equals(method.getName(), "warn") ||
                StringUtils.equals(method.getName(), "info") ||
                StringUtils.equals(method.getName(), "error")
                ) {

            if (StringUtils.isNotEmpty(incMessage)) {
                int i = 0;
                while (i < params.length && !(params[i] instanceof String)) {
                    i++;
                }
                if (i < params.length) {
                    String message = ((String)params[i]).concat(incMessage);
                    params[i] = message;
                }
            }

        }
        return method.invoke(logger, params);
    }

    public static Logger getInstance(Logger logger, Long registryId) {
        InvocationHandler handler = new RegistryLogger(logger, registryId);
        ClassLoader cl = Logger.class.getClassLoader();
        return (Logger) Proxy.newProxyInstance(cl, new Class[]{Logger.class}, handler);
    }
}
