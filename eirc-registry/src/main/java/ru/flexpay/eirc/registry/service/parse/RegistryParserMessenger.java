package ru.flexpay.eirc.registry.service.parse;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

/**
 * @author Pavel Sknar
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
@TransactionAttribute(value= NOT_SUPPORTED)
public class RegistryParserMessenger extends IMessenger {
}
