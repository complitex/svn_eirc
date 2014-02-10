package ru.flexpay.eirc.registry.service.file;

import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.util.FPRegistryConstants;
import ru.flexpay.eirc.registry.util.FileUtil;
import ru.flexpay.eirc.registry.util.RSASignatureUtil;

import javax.ejb.Stateless;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Generate file in FP format.
 * <br/>
 * Content basic logic and similar behaviour.
 */
@Stateless
public class RegistryFPFileService {

	private Logger log = LoggerFactory.getLogger(getClass());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(FPRegistryConstants.OPERATION_DATE_FORMAT);

    private static final String REGISTRY_RECORD_MESSAGE_TYPE = String.valueOf(FPRegistryConstants.REGISTRY_RECORD_MESSAGE_TYPE_CHAR);

    public void writeHeader(DataSource dataSource, ByteBuffer buffer) throws IOException {
        writeLine(buffer, buildHeader(dataSource.getRegistry()), null);
    }

    public void writeRecordsAndFooter(DataSource dataSource, ByteBuffer buffer) throws IOException, AbstractException {

        RegistryRecordData record;
        while ((record = dataSource.getNextRecord()) != null) {
            writeRecord(buffer, dataSource.getRegistry(), record);
        }

        write(buffer, buildFooter(dataSource.getRegistry(), null));
        write(buffer, "\n");
    }

    public void writeAll(DataSource dataSource, ByteBuffer buffer, String privateKey) throws IOException, AbstractException {

        final Signature privateSignature = getPrivateSignature(privateKey);

        writeLine(buffer, buildHeader(dataSource.getRegistry()), privateSignature);

        RegistryRecordData record;
        while ((record = dataSource.getNextRecord()) != null) {
            writeLine(buffer, buildRecord(dataSource.getRegistry(), record), privateSignature);
        }

        write(buffer, buildFooter(dataSource.getRegistry(), privateSignature));
        write(buffer, "\n");
    }

	public String fileName(Registry registry) throws AbstractException {
		return "ree_" + registry.getRegistryNumber() + ".ree_" + registry.getType().getId();
	}

	private Signature getPrivateSignature(String privateKey) throws AbstractException {
		if (privateKey != null) {
			try {
				return RSASignatureUtil.readPrivateSignature(privateKey);
			} catch (Exception e) {
				throw new AbstractException("Error read private signature: " + privateKey, e) {};
			}
		}
		return null;
	}

	protected String buildHeader(Registry registry) {

		StringBuilder header = new StringBuilder();

		log.debug("Building header for registry = {}", registry);

		SimpleDateFormat dfCreation = new SimpleDateFormat(FPRegistryConstants.REGISTRY_CREATION_DATE_FORMAT);
		SimpleDateFormat dfFrom = new SimpleDateFormat(FPRegistryConstants.REGISTRY_DATE_FROM_FORMAT);
		SimpleDateFormat dfTill = new SimpleDateFormat(FPRegistryConstants.REGISTRY_DATE_TILL_FORMAT);

		header.append(FPRegistryConstants.REGISTY_HEADER_MESSAGE_TYPE_CHAR).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getRegistryNumber())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getType().getId())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getRecordsCount())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(dfCreation.format(registry.getCreationDate()))).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(dfFrom.format(registry.getFromDate()))).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(dfTill.format(registry.getTillDate()))).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getSenderOrganizationId())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getRecipientOrganizationId())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getAmount()));
		List<Container> containers = registry.getContainers();
		if (!containers.isEmpty()) {
			header.append(FPRegistryConstants.FIELD_SEPARATOR);
			boolean first = true;
			for (Container container : containers) {
				if (!first) {
					header.append(FPRegistryConstants.CONTAINER_SEPARATOR);
				}
				header.append(container.getData());
				first = false;
			}
		} else {
			if (registry.getType() == RegistryType.BANK_PAYMENTS) {
				header.append(FPRegistryConstants.FIELD_SEPARATOR);
			}
		}

		log.debug("File header = {}", header.toString());
		return header.toString();
	}

	protected String buildRecord(Registry registry, RegistryRecordData record) {

		log.debug("Building string for record = {}", record);

		SimpleDateFormat df = new SimpleDateFormat(FPRegistryConstants.OPERATION_DATE_FORMAT);

		StringBuilder sb = new StringBuilder();
		sb.append(FPRegistryConstants.REGISTRY_RECORD_MESSAGE_TYPE_CHAR).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(registry.getRegistryNumber())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(record.getServiceCode())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(record.getPersonalAccountExt())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				//default town is empty
//				append(StringUtil.valueOf(record.getTownName())).
                append(FPRegistryConstants.ADDRESS_SEPARATOR).
				append(StringUtil.valueOf(record.getStreetType())).
				append(FPRegistryConstants.ADDRESS_SEPARATOR).
				append(StringUtil.valueOf(record.getStreet())).
				append(FPRegistryConstants.ADDRESS_SEPARATOR).
				append(StringUtil.valueOf(record.getBuildingNumber())).
				append(FPRegistryConstants.ADDRESS_SEPARATOR).
				append(StringUtil.valueOf(record.getBuildingCorp())).
				append(FPRegistryConstants.ADDRESS_SEPARATOR).
				append(StringUtil.valueOf(record.getApartment())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(record.getLastName())).
				append(FPRegistryConstants.FIO_SEPARATOR).
				append(StringUtil.valueOf(record.getFirstName())).
				append(FPRegistryConstants.FIO_SEPARATOR).
				append(StringUtil.valueOf(record.getMiddleName())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(df.format(record.getOperationDate()))).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(record.getUniqueOperationNumber())).
				append(FPRegistryConstants.FIELD_SEPARATOR).
				append(StringUtil.valueOf(record.getAmount())).
				append(FPRegistryConstants.FIELD_SEPARATOR);

		int i = 1;
		int total = record.getContainers().size();

		for (Container container : record.getContainers()) {

			sb.append(StringUtil.valueOf(container.getData()));
			if (i != total) {
				sb.append(FPRegistryConstants.CONTAINER_SEPARATOR);
			}

			i++;
		}

		log.debug("File record = {}", sb.toString());

		return sb.toString();

	}

    protected void writeRecord(ByteBuffer buffer, Registry registry, RegistryRecordData record) throws IOException {

        log.debug("Building string for record = {}", record);

        write(buffer, REGISTRY_RECORD_MESSAGE_TYPE);
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(registry.getRegistryNumber()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getServiceCode()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getPersonalAccountExt()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        //default town is empty
        //write(byteBuffer, StringUtil.valueOf(record.getTownName()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getStreetType()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getStreet()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getBuildingNumber()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getBuildingCorp()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getApartment()));
        write(buffer, FPRegistryConstants.ADDRESS_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getRoom()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getLastName()));
        write(buffer, FPRegistryConstants.FIO_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getFirstName()));
        write(buffer, FPRegistryConstants.FIO_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getMiddleName()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(DATE_FORMAT.format(record.getOperationDate())));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getUniqueOperationNumber()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);
        write(buffer, StringUtil.valueOf(record.getAmount()));
        write(buffer, FPRegistryConstants.FIELD_SEPARATOR);

        record.writeContainers(buffer);

        write(buffer, "\n");
    }

	protected String buildFooter(Registry registry, Signature privateSignature) throws IOException {

		StringBuilder footer = new StringBuilder();

		log.debug("Building footer for registry = {}", registry);

		try {
			footer.append(FPRegistryConstants.REGISTRY_FOOTER_MESSAGE_TYPE_CHAR).
					append(FPRegistryConstants.FIELD_SEPARATOR).
					append(StringUtil.valueOf(registry.getRegistryNumber())).
					append(FPRegistryConstants.FIELD_SEPARATOR);
			if (privateSignature != null) {
				footer.append(new String(privateSignature.sign(), FPRegistryConstants.EXPORT_FILE_CHARSET));
			}
		} catch (SignatureException e) {
			throw new IOException("Can not create digital signature", e);
		}

		log.debug("File footer = {}", footer.toString());

		return footer.toString();
	}

    public void writeLine(ByteBuffer buffer, String line, Signature privateSignature) throws IOException {
        FileUtil.writeLine(buffer, line, privateSignature, FPRegistryConstants.EXPORT_FILE_CHARSET);
    }

    public void write(ByteBuffer buffer, String s) throws IOException {
        FileUtil.write(buffer, s, null, FPRegistryConstants.EXPORT_FILE_CHARSET);
    }
}
