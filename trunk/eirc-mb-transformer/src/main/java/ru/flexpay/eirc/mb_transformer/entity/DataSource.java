package ru.flexpay.eirc.mb_transformer.entity;

import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;

import java.io.IOException;

/**
 * @author Pavel Sknar
 */
public interface DataSource {

    Registry getRegistry();

    RegistryRecordData getNextRecord() throws AbstractException, IOException;

}
