package ru.flexpay.eirc.dictionary.web.admin;

import org.complitex.dictionary.entity.IConfig;
import org.complitex.template.web.pages.ConfigEdit;
import ru.flexpay.eirc.dictionary.service.EircConfigBean;

import javax.ejb.EJB;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavel Sknar
 */
public class EircConfigEdit extends ConfigEdit {

    @EJB(name = "EircConfigBean")
    private EircConfigBean configBean;

    @Override
    protected Map<String, List<IConfig>> getConfigGroups() {
        return configBean.getConfigGroups();
    }

    @Override
    protected Set<IConfig> getConfigs() {
        return configBean.getConfigs();
    }
}
