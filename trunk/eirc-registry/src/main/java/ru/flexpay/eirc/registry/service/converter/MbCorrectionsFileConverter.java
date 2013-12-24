package ru.flexpay.eirc.registry.service.converter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.io.PatternFilenameFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.complitex.correction.entity.LinkStatus;
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
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MbCorrectionsFileConverter {

    private static final Logger log = LoggerFactory.getLogger(MbCorrectionsFileConverter.class);

	private static final SimpleDateFormat MODIFICATIONS_START_DATE_FORMAT = new SimpleDateFormat("ddMMyy");
	private static final SimpleDateFormat OPERATION_DATE_FORMAT = new SimpleDateFormat("ddMMyyyy");

    private static final String DELIMITER = "=";

    private static final long FIELDS_LENGTH_SKIP_RECORD = 20;

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

    private RegistryRecordData emptyRecord = new RegistryRecord();

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
                                    File mbFile = new File(dir, fileName);

                                    /*
                                    FileChannel roChannel = new RandomAccessFile(mbFile, "r").getChannel();
                                    ByteBuffer roBuf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
                                    */
                                    convertFile(fileName, new FileInputStream(mbFile), mbFile.length(), imessenger);
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


	public void convertFile(String fileName, InputStream is, long fileLength, IMessenger imessenger) throws AbstractException {

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, MbParsingConstants.REGISTRY_FILE_ENCODING), (int)fileLength);
		} catch (IOException e) {
			throw new MbParseException("Error open file " + fileName, e);
		}

		Registry registry = new Registry();
		initRegistry(registry);

		try {
			parseFile(reader, registry, fileName, fileLength, imessenger);
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
	public void parseFile(final BufferedReader reader, final Registry registry, final String mbFileName,
                          long fileLength, final IMessenger imessenger) throws AbstractException {

        final AtomicInteger lineNum = new AtomicInteger(0);
        final AtomicInteger recordNum = new AtomicInteger(0);

		try {
            reader.skip(MbParsingConstants.FIRST_FILE_STRING_SIZE + 2);
            lineNum.incrementAndGet();

            DataSource dataSource = new DataSource() {

                //Integer flushNumberRegistryRecord = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
                Long mbOrganizationId = configBean.getInteger(RegistryConfig.MB_ORGANIZATION_ID, true).longValue();
                Long eircOrganizationId = configBean.getInteger(RegistryConfig.SELF_ORGANIZATION_ID, true).longValue();
                Queue<RegistryRecordData> recordStack = Queues.newArrayDeque();

                final Context context = new Context(imessenger, mbOrganizationId, eircOrganizationId, "ХАРЬКОВ");

                int countChar = 0;

                @Override
                public Registry getRegistry() {
                    return registry;
                }

                @Override
                public RegistryRecordData getNextRecord() throws AbstractException, IOException {

                    if (!recordStack.isEmpty()) {
                        Context.RegistryRecordMapped registryRecord = (Context.RegistryRecordMapped)recordStack.poll();
                        registryRecord.setNotUsing();

                        return registryRecord;
                    }

                    if (lineNum.get()%10000 == 0) {
                        imessenger.addMessageInfo("processed_lines", lineNum.get(), mbFileName);
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
                        parseHeader(line.split(DELIMITER), registry, context);
                        line = reader.readLine();
                    }
                    int count;
                    do {
                        if (line.startsWith(MbParsingConstants.LAST_FILE_STRING_BEGIN) || line == null) {
                            registry.setRecordsCount(recordNum.get());
                            registry.setFromDate(context.getFromDate());
                            registry.setTillDate(context.getTillDate());
                            log.info("Total {} records created", recordNum.get());
                            countChar = -1;
                            return null;
                        }

                        count = parseRecord(line, recordStack, context);

                        if (count == 0) {
                            line = reader.readLine();
                        } else {
                            recordNum.addAndGet(count);
                        }

                        lineNum.incrementAndGet();
                    } while (count == 0);

                    Context.RegistryRecordMapped registryRecord = (Context.RegistryRecordMapped)recordStack.poll();
                    registryRecord.setNotUsing();

                    return registryRecord;
                }
			};

            /*
            RegistryRecordData record;
            while ((record = dataSource.getNextRecord()) != null) {
                ((Context.RegistryRecordMapped)record).setNotUsing();
            }
            */

            final String dir = configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true);
            final String tmpDir = configBean.getString(RegistryConfig.TMP_DIR, true);

            String eircFileName = registryFPFileFormat.fileName(registry);
            File tmpFile = new File(tmpDir, eircFileName + "_tmp");

            FileChannel rwChannel = null;
            FileChannel outChannel = null;
            try {
                //  Create a read-writeContainers memory-mapped file
                rwChannel = new RandomAccessFile(tmpFile, "rw").getChannel();
                ByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileLength*2);

                registryFPFileFormat.writeRecordsAndFooter(dataSource, buffer);

                // Create registry file
                outChannel = new FileOutputStream(new File(dir, eircFileName)).getChannel();
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

            imessenger.addMessageInfo("total_lines", lineNum.get(), mbFileName, eircFileName);

		} catch (IOException e) {
			throw new MbParseException("Error reading file ", e);
		}

	}

	private void initRegistry(Registry registry) {
		registry.setCreationDate(DateUtil.getCurrentDate());
        registry.setRegistryNumber(DateUtil.getCurrentDate().getTime());
		registry.setType(RegistryType.SALDO_SIMPLE);
	}

	private void parseHeader(String[] fields, Registry registry, Context context) throws AbstractException {
		log.debug("fields: {}", fields);
		log.debug("Getting service provider with id = {} from DB", fields[1]);
		List<OrganizationCorrection> organizationCorrections = organizationCorrectionBean.getOrganizationCorrections(
                FilterWrapper.of(new OrganizationCorrection(null, null, fields[1],
                        context.getMbOrganizationId(), context.getEircOrganizationId(), null)));
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
		registry.setRecipientOrganizationId(context.getEircOrganizationId());

		try {
			Date period = new SimpleDateFormat(MbParsingConstants.FILE_CREATION_DATE_FORMAT).parse(fields[2]);
            registry.setCreationDate(period);
		} catch (ParseException e) {
            //
		}
	}

	private int parseRecord(String line, Queue<RegistryRecordData> recordStack, Context context) throws AbstractException {
		log.debug("Parse line: {}", line);

		String[] fields = line.split(DELIMITER);

		if (fields.length == FIELDS_LENGTH_SKIP_RECORD) {
			log.debug("Skip record: {}", line);
			return 0;
		}

		if (fields.length > FIELDS_LENGTH_SKIP_RECORD &&
				StringUtils.isEmpty(fields[9]) &&
				StringUtils.isEmpty(fields[10]) &&
				StringUtils.isEmpty(fields[19])) {
			fields = (String[]) ArrayUtils.remove(fields, 9);
			fields[9] = "-";
		}

        if (StringUtils.isNotEmpty(fields[9])) {
            fields[9] = StringUtils.replace(fields[9], ";", "\\;");
        }

		// remove duplicates in service codes
		Set<String> serviceCodes = ImmutableSet.<String>builder().add(fields[20].split(";")).build();

		int count = 0;
		for (String serviceCode : serviceCodes) {
			if (StringUtils.isEmpty(serviceCode) || "0".equals(serviceCode)) {
				return 0;
			}
			RegistryRecordData record = context.getRegistryRecord(fields, serviceCode);

            recordStack.add(record);
            count++;

			//In processing operation check if consumer already exists and does not create account
            /*
			if (!record.getContainers().isEmpty()) {
				++count;
                recordStack.add(record);
			}*/
		}

		return count;
	}

	protected String[] parseBuildingAddress(String mbBuidingAddress) {
		String[] parts = StringUtils.split(mbBuidingAddress, ' ');
		if (parts.length > 1 && parts[1].startsWith(MbParsingConstants.BUILDING_BULK_PREFIX)) {
			parts[1] = parts[1].substring(MbParsingConstants.BUILDING_BULK_PREFIX.length());
		}
		return parts;
	}

	private String getErcAccount(String[] fields) {
		return fields[0];
	}

    private class Context {

        private IMessenger imessenger;
        private List<RegistryRecordData> registryRecords = Lists.newLinkedList();

        private Long mbOrganizationId;
        private Long eircOrganizationId;
        private String city;
        private Date fromDate;
        private Date tillDate;

        private Cache<String, String> serviceCache = CacheBuilder.newBuilder().
                maximumSize(1000).
                expireAfterWrite(10, TimeUnit.MINUTES).
                build();

        public Context(IMessenger imessenger, Long mbOrganizationId, Long eircOrganizationId, String city) {
            this.imessenger = imessenger;
            this.mbOrganizationId = mbOrganizationId;
            this.eircOrganizationId = eircOrganizationId;
            this.city = city;
        }

        public IMessenger getIMessenger() {
            return imessenger;
        }

        public Long getMbOrganizationId() {
            return mbOrganizationId;
        }

        public Long getEircOrganizationId() {
            return eircOrganizationId;
        }

        public Date getFromDate() {
            return fromDate;
        }

        public Date getTillDate() {
            return tillDate;
        }

        public RegistryRecordData getRegistryRecord(String[] fields, String serviceCode) throws MbParseException {
            for (RegistryRecordData registryRecord : registryRecords) {
                if (!((RegistryRecordMapped)registryRecord).isUsing()) {
                    ((RegistryRecordMapped) registryRecord).initData(fields, serviceCode);
                    return registryRecord;
                }
            }
            RegistryRecordMapped registryRecord = new RegistryRecordMapped(fields, serviceCode);
            registryRecords.add(registryRecord);

            return registryRecord;
        }

        private class RegistryRecordMapped implements RegistryRecordData {

            private String[] fields;
            private String[] buildingFields;
            private String serviceCode;
            private Date modificationDate;

            private boolean using = false;

            private RegistryRecordMapped(String[] fields, String serviceCode) throws MbParseException {
                initData(fields, serviceCode);
            }

            public void initData(String[] fields, String serviceCode) throws MbParseException {
                this.fields = fields;
                this.serviceCode = serviceCache.getIfPresent(serviceCode);
                if (this.serviceCode == null) {
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
                    this.serviceCode = String.valueOf(serviceCorrections.get(0).getObjectId());
                    serviceCache.put(serviceCode, this.serviceCode);
                }
                try {
                    this.modificationDate = MODIFICATIONS_START_DATE_FORMAT.parse(fields[19]);
                } catch (ParseException e) {
                    throw new MbParseException("Failed parse modification start date", e);
                }
                if (fromDate == null || this.modificationDate.before(fromDate)) {
                    fromDate = this.modificationDate;
                }
                if (tillDate == null || this.modificationDate.after(tillDate)) {
                    tillDate = this.modificationDate;
                }
                buildingFields = parseBuildingAddress(fields[8]);
                using = true;
            }

            public boolean isUsing() {
                return using;
            }

            public void setNotUsing() {
                this.using = false;
            }

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public void setId(Long id) {

            }

            @Override
            public String getServiceCode() {
                return serviceCode;
            }

            @Override
            public String getPersonalAccountExt() {
                return fields[1];
            }

            @Override
            public String getFirstName() {
                return "";
            }

            @Override
            public String getMiddleName() {
                return "";
            }

            @Override
            public String getLastName() {
                return fields[2];
            }

            @Override
            public Date getOperationDate() {
                return new Date();
            }

            @Override
            public Long getUniqueOperationNumber() {
                return null;
            }

            @Override
            public BigDecimal getAmount() {
                return null;
            }

            @Override
            public RegistryRecordStatus getStatus() {
                return null;
            }

            @Override
            public List<Container> getContainers() {
                return null;
            }

            @Override
            public ImportErrorType getImportErrorType() {
                return null;
            }

            @Override
            public Long getRegistryId() {
                return null;
            }

            @Override
            public Long getCityTypeId() {
                return null;
            }

            @Override
            public Long getCityId() {
                return null;
            }

            @Override
            public Long getStreetTypeId() {
                return null;
            }

            @Override
            public Long getStreetId() {
                return null;
            }

            @Override
            public Long getBuildingId() {
                return null;
            }

            @Override
            public Long getApartmentId() {
                return null;
            }

            @Override
            public void addContainer(Container container) {

            }

            @Override
            public Address getAddress() {
                return null;
            }

            @Override
            public Person getPerson() {
                return null;
            }

            @Override
            public String getStreetCode() {
                return null;
            }

            @Override
            public void writeContainers(ByteBuffer buffer) {
                String operationDate = OPERATION_DATE_FORMAT.format(modificationDate);

                write(buffer, ContainerType.OPEN_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                write(buffer, ContainerType.EXTERNAL_ORGANIZATION_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, getErcAccount(fields));
                write(buffer, ":");
                write(buffer, mbOrganizationId);

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // ФИО
                write(buffer, ContainerType.SET_RESPONSIBLE_PERSON.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, fields[2]);

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // Количество проживающих
                String containerValue = StringUtils.isEmpty(fields[15])? "0": fields[15];
                write(buffer, ContainerType.SET_NUMBER_ON_HABITANTS.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, containerValue);

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // Отапливаемая площадь
                containerValue = StringUtils.isEmpty(fields[10])? "0.00": fields[10];
                write(buffer, ContainerType.SET_WARM_SQUARE.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, containerValue);
            }

            @Override
            public void setCityTypeId(Long id) {

            }

            @Override
            public void setCityId(Long id) {

            }

            @Override
            public void setStreetTypeId(Long id) {

            }

            @Override
            public void setStreetId(Long id) {

            }

            @Override
            public void setBuildingId(Long id) {

            }

            @Override
            public void setApartmentId(Long id) {

            }

            @Override
            public <T extends LinkStatus> void setStatus(T status) {

            }

            @Override
            public String getCityType() {
                return null;
            }

            @Override
            public String getCity() {
                return city;
            }

            @Override
            public String getStreetType() {
                return fields[6];
            }

            @Override
            public String getStreetTypeCode() {
                return null;
            }

            @Override
            public String getStreet() {
                return fields[7];
            }

            @Override
            public String getBuildingNumber() {
                return buildingFields[0];
            }

            @Override
            public String getBuildingCorp() {
                return buildingFields.length > 1? buildingFields[1] : null;
            }

            @Override
            public String getApartment() {
                return fields[9];
            }

            private void write(ByteBuffer buffer, String s) {
                buffer.put(getEncodingBytes(s));
            }

            private void write(ByteBuffer buffer, long value) {
                buffer.put(getEncodingBytes(value));
            }

            private byte[] getEncodingBytes(String s) {
                return s.getBytes(Charset.forName(FPRegistryConstants.EXPORT_FILE_ENCODING));
            }

            private byte[] getEncodingBytes(long value) {
                return getEncodingBytes(Long.toString(value));
            }
        }
    }
}
