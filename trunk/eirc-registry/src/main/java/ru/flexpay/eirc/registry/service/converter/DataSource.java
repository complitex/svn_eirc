package ru.flexpay.eirc.registry.service.converter;

import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;

import java.io.IOException;

/**
 * @author Pavel Sknar
 */
public interface DataSource {

    Registry getRegistry();

    RegistryRecord getNextRecord() throws AbstractException, IOException;

}
