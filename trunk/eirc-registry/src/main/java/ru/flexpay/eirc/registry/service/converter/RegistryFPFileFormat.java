package ru.flexpay.eirc.registry.service.converter;

import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecord;
import ru.flexpay.eirc.registry.entity.RegistryType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
public class RegistryFPFileFormat {

	private Logger log = LoggerFactory.getLogger(getClass());

    @EJB
	private RSASignatureService signatureService;

    public void writeHeader(DataSource dataSource, ByteBuffer buffer) throws IOException {
        writeLine(buffer, buildHeader(dataSource.getRegistry()), null);
    }

    public void writeRecordsAndFooter(DataSource dataSource, ByteBuffer buffer) throws IOException, AbstractException {

        RegistryRecord record;
        while ((record = dataSource.getNextRecord()) != null) {
            writeLine(buffer, buildRecord(dataSource.getRegistry(), record), null);
        }

        buffer.put(getEncodingBytes(buildFooter(dataSource.getRegistry(), null)));
        buffer.put(getEncodingBytes("\n"));
    }

    public void writeAll(DataSource dataSource, ByteBuffer buffer, String privateKey) throws IOException, AbstractException {

        final Signature privateSignature = getPrivateSignature(privateKey);

        writeLine(buffer, buildHeader(dataSource.getRegistry()), privateSignature);

        RegistryRecord record;
        while ((record = dataSource.getNextRecord()) != null) {
            writeLine(buffer, buildRecord(dataSource.getRegistry(), record), privateSignature);
        }

        buffer.put(getEncodingBytes(buildFooter(dataSource.getRegistry(), privateSignature)));
        buffer.put(getEncodingBytes("\n"));
    }

    private void writeLine(ByteBuffer buffer, String line, Signature privateSignature) throws IOException {
        if (privateSignature != null) {
            try {
                privateSignature.update(line.getBytes());
                privateSignature.update("\n".getBytes());
            } catch (SignatureException e) {
                throw new IOException("Can not update signature", e);
            }
        }
        buffer.put(getEncodingBytes(line));
        buffer.put(getEncodingBytes("\n"));
    }

    private byte[] getEncodingBytes(String s) {
        return s.getBytes(Charset.forName(FPRegistryConstants.EXPORT_FILE_ENCODING));
    }

	public String fileName(Registry registry) throws AbstractException {
		return "ree_" + registry.getRegistryNumber() + ".ree_" + registry.getType().getId();
	}

	private Signature getPrivateSignature(String privateKey) throws AbstractException {
		if (privateKey != null) {
			try {
				return signatureService.readPrivateSignature(privateKey);
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

	protected String buildRecord(Registry registry, RegistryRecord record) {

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

	protected String buildFooter(Registry registry, Signature privateSignature) throws IOException {

		StringBuilder footer = new StringBuilder();

		log.debug("Building footer for registry = {}", registry);

		try {
			footer.append(FPRegistryConstants.REGISTRY_FOOTER_MESSAGE_TYPE_CHAR).
					append(FPRegistryConstants.FIELD_SEPARATOR).
					append(StringUtil.valueOf(registry.getRegistryNumber())).
					append(FPRegistryConstants.FIELD_SEPARATOR);
			if (privateSignature != null) {
				footer.append(new String(privateSignature.sign(), FPRegistryConstants.EXPORT_FILE_ENCODING));
			}
		} catch (SignatureException e) {
			throw new IOException("Can not create digital signature", e);
		}

		log.debug("File footer = {}", footer.toString());

		return footer.toString();
	}
}
