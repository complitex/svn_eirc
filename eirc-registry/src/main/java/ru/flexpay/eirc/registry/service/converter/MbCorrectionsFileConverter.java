package ru.flexpay.eirc.registry.service.converter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Queues;
import com.google.common.io.PatternFilenameFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.complitex.correction.entity.OrganizationCorrection;
import org.complitex.correction.service.OrganizationCorrectionBean;
import org.complitex.dictionary.entity.DictionaryConfig;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.AbstractJob;
import ru.flexpay.eirc.registry.service.FinishCallback;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.handle.MbConverterQueueProcessor;
import ru.flexpay.eirc.service.entity.ServiceCorrection;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class MbCorrectionsFileConverter {

    private static final Logger log = LoggerFactory.getLogger(MbCorrectionsFileConverter.class);

	private static final String MODIFICATIONS_START_DATE_FORMAT = "ddMMyy";

    private static final String DELIMITER = "=";

    private static final long FIELDS_LENGTH = 28;
    private static final long FIELDS_LENGTH_SKIP_RECORD = 20;
    private static final long FIELDS_LENGTH_EMPTY_FOOTER = 21;

    private static final long BUFFER_SIZE = 1_048_576*10;

    @EJB
    private OrganizationCorrectionBean organizationCorrectionBean;

    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ConfigBean configBean;

    @EJB
    private EircOrganizationStrategy organizationStrategy;

    @EJB
    private RegistryFPFileFormat registryFPFileFormat;

    @EJB
    private MbConverterQueueProcessor mbConverterQueueProcessor;

    public void convert(final IMessenger imessenger, final FinishCallback finishConvert) throws ExecutionException {
        imessenger.addMessageInfo("mb_registry_convert_starting");
        finishConvert.init();

        mbConverterQueueProcessor.execute(
                new AbstractJob<Void>() {
                    @Override
                    public Void execute() throws ExecuteException {
                        try {

                            final String dir = configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true);

                            String[] fileNames = new File(dir).list(new PatternFilenameFilter(".+\\.kor"));

                            for (String fileName : fileNames) {
                                try {
                                    convertFile(fileName, new FileInputStream(new File(dir, fileName)));
                                } catch (Exception e) {
                                    log.error("Can not convert file " + fileName, e);
                                    imessenger.addMessageError("mb_registry_fail_convert", fileName,
                                            e.getMessage() != null ? e.getMessage() : e.getCause().getMessage());
                                }
                            }
                            return null;
                        } finally {
                            imessenger.addMessageInfo("mb_registry_convert_finish");
                            finishConvert.complete();
                        }
                    }
                }
        );
    }


	public void convertFile(String fileName, InputStream is) throws AbstractException {
		Logger plog = log;

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, MbParsingConstants.REGISTRY_FILE_ENCODING));
		} catch (IOException e) {
			throw new MbParseException("Error open file " + fileName, e);
		}

		Registry registry = new Registry();
		initRegistry(registry);

		try {
			parseFile(reader, registry);
		} finally {
			IOUtils.closeQuietly(reader);
		}


		//if (plog.isInfoEnabled()) {
		//	plog.info("Registry parse completed, total lines {}, total records {}",
		//			new Object[]{parameters.get(ParserParameterConstants.PARAM_TOTAL_LINE_NUM),
		//						parameters.get(ParserParameterConstants.PARAM_TOTAL_RECORD_NUM)});
		//}

	}

	@SuppressWarnings ({"unchecked"})
	public void parseFile(final BufferedReader reader, final Registry registry) throws AbstractException {

        final AtomicInteger lineNum = new AtomicInteger(0);
        final AtomicInteger recordNum = new AtomicInteger(0);

		try {
            reader.skip(MbParsingConstants.FIRST_FILE_STRING_SIZE + 2);
            lineNum.incrementAndGet();

            DataSource dataSource = new DataSource() {

                //Integer flushNumberRegistryRecord = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                Long mbOrganizationId = configBean.getInteger(RegistryConfig.MB_ORGANIZATION_ID, true).longValue();
                Long eircOrganizationId = configBean.getInteger(RegistryConfig.SELF_ORGANIZATION_ID, true).longValue();
                Queue<RegistryRecord> recordStack = Queues.newArrayDeque();

                int countChar = 0;

                @Override
                public Registry getRegistry() {
                    return registry;
                }

                @Override
                public RegistryRecord getNextRecord() throws AbstractException, IOException {

                    if (!recordStack.isEmpty()) {
                        return recordStack.poll();
                    }

                    String line = reader.readLine();
                    //log.debug("totalLineNum={}, line: {}", new Object[]{totalLineNum, line});
                    if (line == null) {
                        log.debug("End of file, lineNum = {}", lineNum.get());
                        countChar = -1;
                        return null;
                    }
                    countChar += line.length() + 2;
                    if (lineNum.get() == 1) {
                        parseHeader(line.split(DELIMITER), registry, mbOrganizationId, eircOrganizationId);
                        line = reader.readLine();
                    }
                    if (line.startsWith(MbParsingConstants.LAST_FILE_STRING_BEGIN)) {
                        registry.setRecordsCount(recordNum.get());
                        log.info("Total {} records created", recordNum.get());
                        countChar = -1;
                        return null;
                    } else {
                        recordNum.addAndGet(parseRecord(line, mbOrganizationId, eircOrganizationId, recordStack));
                    }
                    lineNum.incrementAndGet();

                    return recordStack.poll();
                }
			};

            final String dir = configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true);
            final String tmpDir = configBean.getString(RegistryConfig.TMP_DIR, true);

            String fileName = registryFPFileFormat.fileName(registry);
            File tmpFile = new File(tmpDir, fileName + "_tmp");

            FileChannel rwChannel = null;
            FileChannel outChannel = null;
            try {
                //  Create a read-write memory-mapped file
                rwChannel = new RandomAccessFile(tmpFile, "rw").getChannel();
                ByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);

                registryFPFileFormat.writeRecordsAndFooter(dataSource, buffer);

                // Create registry file
                outChannel = new FileOutputStream(new File(dir, fileName)).getChannel();
                ByteBuffer buff = ByteBuffer.allocateDirect(32 * 1024);

                registryFPFileFormat.writeHeader(dataSource, buff);

                buff.flip();
                outChannel.write(buff);
                buff.clear();

                buffer.flip();
                outChannel.write(buffer);
                buffer.clear();
            } finally {
                IOUtils.closeQuietly(outChannel);
                IOUtils.closeQuietly(rwChannel);

                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }


		} catch (IOException e) {
			throw new MbParseException("Error reading file ", e);
		}

	}

	private void initRegistry(Registry registry) {
		registry.setCreationDate(DateUtil.getCurrentDate());
        registry.setRegistryNumber(DateUtil.getCurrentDate().getTime());
		registry.setType(RegistryType.SALDO_SIMPLE);
	}

	private void parseHeader(String[] fields, Registry registry, Long mbOrganizationId, Long eircOrganizationId) throws AbstractException {
		log.debug("fields: {}", fields);
		log.debug("Getting service provider with id = {} from DB", fields[1]);
		List<OrganizationCorrection> organizationCorrections = organizationCorrectionBean.getOrganizationCorrections(
                FilterWrapper.of(new OrganizationCorrection(null, null, fields[1], mbOrganizationId, eircOrganizationId, null)));
		if (organizationCorrections.size() <= 0) {
			throw new MbParseException("No service provider correction with id {0}", fields[1]);
		}
        if (organizationCorrections.size() > 1) {
            throw new MbParseException("Found several correction for service provider {0}", fields[1]);
        }
		Organization organization = organizationStrategy.findById(organizationCorrections.get(0).getObjectId(), false);
		if (organization == null) {
			throw new MbParseException(
                    "Incorrect header line (can't find service provider with id {0})",
                    registry.getSenderOrganizationId());
		}

		registry.setSenderOrganizationId(organization.getId());
		registry.setRecipientOrganizationId(eircOrganizationId);

		try {
			Date period = new SimpleDateFormat(MbParsingConstants.FILE_CREATION_DATE_FORMAT).parse(fields[2]);
			registry.setFromDate(period);
			registry.setTillDate(period);
		} catch (ParseException e) {
			// do nothing
		}
	}

	private int parseRecord(String line, Long mbOrganizationId, Long eircOrganizationId, Queue<RegistryRecord> recordStack) throws AbstractException {
		log.debug("Parse line: {}", line);

		String[] fields = line.split(DELIMITER);

		if (fields.length == FIELDS_LENGTH_SKIP_RECORD) {
			log.debug("Skip record: {}", line);
			return 0;
		}

		if (fields.length > FIELDS_LENGTH_SKIP_RECORD &&
				StringUtils.isEmpty(fields[9]) &&
				StringUtils.isEmpty(fields[10]) &&
				StringUtils.isEmpty(getModificationDate(fields[19]))) {
			fields = (String[]) ArrayUtils.remove(fields, 9);
			fields[9] = "-";
		}

		// remove duplicates in service codes
		Set<String> serviceCodes = ImmutableSet.<String>builder().add(fields[20].split(";")).build();

		int count = 0;
		for (String serviceCode : serviceCodes) {
			if (StringUtils.isEmpty(serviceCode) || "0".equals(serviceCode)) {
				return 0;
			}
			RegistryRecord record = newRecord(fields, serviceCode, mbOrganizationId, eircOrganizationId);

			//In processing operation check if consumer already exists and does not create account
			addCreateAccountContainer(record, fields);

			setInfoContainers(record, fields, mbOrganizationId);
			if (!record.getContainers().isEmpty()) {
				++count;
                recordStack.add(record);
			}
		}

		return count;
	}

	private void setBuildingAddress(RegistryRecord record, String addr) {
		String[] parts = parseBuildingAddress(addr);
		record.setBuildingNumber(parts[0]);
		if (parts.length > 1) {
			record.setBuildingCorp(parts[1]);
		}
	}

	protected String[] parseBuildingAddress(String mbBuidingAddress) {
		String[] parts = StringUtils.split(mbBuidingAddress, ' ');
		if (parts.length > 1 && parts[1].startsWith(MbParsingConstants.BUILDING_BULK_PREFIX)) {
			parts[1] = parts[1].substring(MbParsingConstants.BUILDING_BULK_PREFIX.length());
		}
		return parts;
	}

	private String getModificationDate(String field) {

		try {
			return new SimpleDateFormat("ddMMyyyy").format(
					new SimpleDateFormat(MODIFICATIONS_START_DATE_FORMAT).parse(field));
		} catch (ParseException e) {
			return "";
		}
	}

	private long addCreateAccountContainer(RegistryRecord record, String[] fields) {

		String modificationStartDate = getModificationDate(fields[19]);
		Container container = new Container(ContainerType.OPEN_ACCOUNT.getId() + ":" + modificationStartDate + "::", ContainerType.OPEN_ACCOUNT);
		record.addContainer(container);

		return 1;
	}

	private String getErcAccount(String[] fields) {
		return fields[0];
	}

	private RegistryRecord setInfoContainers(RegistryRecord record, String[] fields, Long mbOrganizationId) {

		String modificationStartDate = getModificationDate(fields[19]);

        record.addContainer(
                new Container(ContainerType.EXTERNAL_ORGANIZATION_ACCOUNT.getId() + ":" + modificationStartDate + "::" +
                        getErcAccount(fields) + ":" + mbOrganizationId,
                        ContainerType.EXTERNAL_ORGANIZATION_ACCOUNT)
        );

		// ФИО
		record.addContainer(
                new Container(ContainerType.SET_RESPONSIBLE_PERSON.getId() + ":" + modificationStartDate + "::" + fields[2],
                        ContainerType.SET_RESPONSIBLE_PERSON)
        );

		// Количество проживающих
		String containerValue = StringUtils.isEmpty(fields[15])? "0": fields[15];
		record.addContainer(
                new Container(ContainerType.SET_NUMBER_ON_HABITANTS.getId() + ":" + modificationStartDate + "::" + containerValue,
                        ContainerType.SET_NUMBER_ON_HABITANTS)
        );

		// Отапливаемая площадь
		containerValue = StringUtils.isEmpty(fields[10])? "0.00": fields[10];
        record.addContainer(
                new Container(ContainerType.SET_WARM_SQUARE.getId() + ":" + modificationStartDate + "::" + containerValue,
                        ContainerType.SET_WARM_SQUARE)
        );


		// Тип льготы
		/*
		if (StringUtils.isNotEmpty(fields[17]) && !"0".equals(fields[17])) {
			container = new RegistryRecordContainer();
			container.setData("8:" + modificationStartDate + "::" + fields[17]);
			record.addContainer(container);
		}
             */
		// ФИО носителя льготы
		/*
		if (fields.length != CorrectionsRecordValidator.FIELDS_LENGTH_EMPTY_FOOTER &&
				StringUtils.isNotEmpty(fields[26]) && !"0".equals(fields[26])) {
			container = new RegistryRecordContainer();
			container.setData("9:" + modificationStartDate + "::" + fields[26]);
			record.addContainer(container);
		}
		*/

		// Количество пользующихся льготой
		/*
		if (StringUtils.isNotEmpty(fields[16]) && !"0".equals(fields[16])) {
			container = new RegistryRecordContainer();
			container.setData("12:" + modificationStartDate + "::" + fields[16]);
			record.addContainer(container);
		}
        	*/
		
		return record;
	}

	private RegistryRecord newRecord(String[] fields, String serviceCode, Long mbOrganizationId, Long eircOrganizationId)
            throws AbstractException {

        RegistryRecord record = new RegistryRecord();

		setServiceCode(record, serviceCode, mbOrganizationId, eircOrganizationId);
		record.setPersonalAccountExt(fields[1]);
		record.setOperationDate(new Date());

		record.setLastName(fields[2]);
		record.setMiddleName("");
		record.setFirstName("");
		record.setCity("ХАРЬКОВ");
		record.setStreetType(fields[6]);
		record.setStreet(fields[7]);
		setBuildingAddress(record, fields[8]);
		record.setApartment(fields[9]);
		record.getContainers().clear();



		return record;
	}

    private void setServiceCode(RegistryRecord record, String serviceCode, Long mbOrganizationId, Long eircOrganizationId)
            throws AbstractException {
        List<ServiceCorrection> serviceCorrections = serviceCorrectionBean.getServiceCorrections(
                FilterWrapper.of(new ServiceCorrection(null, null, serviceCode, mbOrganizationId, eircOrganizationId, null))
        );
        if (serviceCorrections.size() <= 0) {
            throw new MbParseException(
                    "No found service correction with code {0}", serviceCode);
        }
        if (serviceCorrections.size() > 1) {
            throw new MbParseException("Found several correction for service with code {0}", serviceCode);
        }
        record.setServiceCode(String.valueOf(serviceCorrections.get(0).getObjectId()));
    }
}
