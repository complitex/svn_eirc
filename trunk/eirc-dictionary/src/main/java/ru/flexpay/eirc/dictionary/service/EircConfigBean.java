package ru.flexpay.eirc.dictionary.service;

import com.google.common.collect.ImmutableSet;
import org.complitex.dictionary.entity.IConfig;
import org.complitex.dictionary.service.ConfigBean;
import ru.flexpay.eirc.dictionary.entity.EircConfig;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Set;

/**
 * @author Pavel Sknar
 */
@Startup
@Singleton(name = "EircConfigBean")
public class EircConfigBean extends ConfigBean {
    @Override
    protected Set<Class<? extends IConfig>> getIConfigClasses() {
        return ImmutableSet.<Class<? extends IConfig>>of(EircConfig.class);
    }
}
