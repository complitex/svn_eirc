package ru.flexpay.eirc.mb_transformer.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.PatternFilenameFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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
import org.complitex.dictionary.util.EjbBeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.mb_transformer.entity.Context;
import ru.flexpay.eirc.mb_transformer.entity.DataSource;
import ru.flexpay.eirc.mb_transformer.entity.RegistryRecordMapped;
import ru.flexpay.eirc.mb_transformer.util.FPRegistryConstants;
import ru.flexpay.eirc.mb_transformer.util.MbParsingConstants;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.AbstractJob;
import ru.flexpay.eirc.registry.service.FinishCallback;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.handle.MbConverterQueueProcessor;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class MbCorrectionsFileConverter {

    private static final Logger log = LoggerFactory.getLogger(MbCorrectionsFileConverter.class);

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

                            String[] fileNames = new File(dir).list(new PatternFilenameFilter(".+\\.(kor|nac)"));
                            Arrays.sort(fileNames);

                            Map<String, MbFile> chargesFiles = Maps.newHashMap();
                            Map<String, MbFile> correctionsFiles = Maps.newHashMap();

                            getMbFiles(dir, fileNames, chargesFiles, correctionsFiles);

                            for (Map.Entry<String, MbFile> fileEntry : chargesFiles.entrySet()) {
                                if (!correctionsFiles.containsKey(fileEntry.getKey())) {
                                    log.error("mb_registry_fail_convert", fileEntry.getKey(), "corrections file not found");
                                    continue;
                                }

                                try {
                                    EjbBeanLocator.getBean(MbCorrectionsFileConverter.class).
                                            convertFile(correctionsFiles.get(fileEntry.getKey()), fileEntry.getValue(), imessenger);
                                } catch (Exception e) {
                                    log.error("Can not convert file " + fileEntry.getKey(), e);
                                    imessenger.addMessageError("mb_registry_fail_convert", fileEntry.getKey(),
                                            e.getMessage() != null ? e.getMessage() : e.getCause().getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Can not convert files", e);
                            imessenger.addMessageError("mb_registries_fail_convert",
                                    e.getMessage() != null ? e.getMessage() : e.getCause().getMessage());
                        } finally {
                            imessenger.addMessageInfo("mb_registry_convert_finish");
                            finishConvert.complete();
                        }
                        return null;
                    }
                }
        );
    }

    private void getMbFiles(String dir, String[] fileNames, Map<String, MbFile> chargesFiles,
                            Map<String, MbFile> correctionsFiles) throws FileNotFoundException {

        for (String fileName : fileNames) {
            MbFile mbFile = new MbFile(dir, fileName);
            switch (mbFile.getFileType()) {
                case CHARGES:
                    chargesFiles.put(mbFile.getShortName(), mbFile);
                    break;
                case CORRECTIONS:
                    correctionsFiles.put(mbFile.getShortName(), mbFile);
                    break;
            }
        }
    }

    private BufferedReader getBufferedReader(MbFile mbFile) throws MbParseException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(mbFile.getInputStream(), MbParsingConstants.REGISTRY_FILE_ENCODING), (int)mbFile.getFileLength());
        } catch (IOException e) {
            throw new MbParseException("Error open file " + mbFile.getFileName(), e);
        }
        return reader;
    }

    @SuppressWarnings ({"unchecked"})
	public void convertFile(final MbFile correctionsFile, final MbFile chargesFile, final IMessenger imessenger) throws AbstractException {

        final Registry registry = new Registry();
        initRegistry(registry);

        final AtomicInteger lineNum = new AtomicInteger(0);
        final AtomicInteger recordNum = new AtomicInteger(0);

        final Map<String, int[]> chargesContainers = Maps.newHashMapWithExpectedSize(((int)chargesFile.fileLength)/30);

		try {


            MbContextDataSource dataSource = new MbContextDataSource() {

                Queue<RegistryRecordData> recordStack = Queues.newArrayDeque();

                int countChar = 0;

                @Override
                public Registry getRegistry() {
                    return registry;
                }

                @Override
                public RegistryRecordData getNextRecord() throws AbstractException, IOException {

                    if (!recordStack.isEmpty()) {
                        RegistryRecordMapped registryRecord = (RegistryRecordMapped)recordStack.poll();
                        registryRecord.setNotUsing();

                        return registryRecord;
                    }

                    if (lineNum.get()%10000 == 0) {
                        imessenger.addMessageInfo("processed_lines", lineNum.get(), getFileName());
                    }

                    String line = readLine();
                    //log.debug("totalLineNum={}, line: {}", new Object[]{totalLineNum, line});
                    if (line == null) {
                        log.debug("End of file, lineNum = {}", lineNum.get());
                        countChar = -1;
                        return null;
                    }
                    countChar += line.length() + 2;
                    if (lineNum.get() == 1) {
                        parseHeader(line.split(MbParsingConstants.DELIMITER), registry, getContext());
                        line = readLine();
                    }

                    int count;
                    do {
                        if (line.startsWith(MbParsingConstants.LAST_FILE_STRING_BEGIN) || line == null) {
                            registry.setRecordsCount(recordNum.get());
                            log.info("Total {} records created", recordNum.get());
                            countChar = -1;
                            return null;
                        }

                        count = parseRecord(line, recordStack, getContext());

                        if (count == 0) {
                            line = readLine();
                        } else {
                            recordNum.addAndGet(count);
                        }

                        lineNum.incrementAndGet();
                    } while (count == 0);

                    RegistryRecordMapped registryRecord = (RegistryRecordMapped)recordStack.poll();
                    registryRecord.setNotUsing();

                    return registryRecord;
                }

                @Override
                public void initContextDataSource(Context context, BufferedReader reader, String fileName) throws IOException {
                    super.initContextDataSource(context, reader, fileName);
                    reader.skip(MbParsingConstants.FIRST_FILE_STRING_SIZE + 2);
                    recordNum.set(0);
                    lineNum.set(0);
                    lineNum.incrementAndGet();
                }
            };

            Long mbOrganizationId = configBean.getInteger(RegistryConfig.MB_ORGANIZATION_ID, true).longValue();
            Long eircOrganizationId = configBean.getInteger(RegistryConfig.SELF_ORGANIZATION_ID, true).longValue();

            final String dir = configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true);
            final String tmpDir = configBean.getString(RegistryConfig.TMP_DIR, true);

            File chargeContainersFile = new File(tmpDir, DateUtil.getCurrentDate().getTime() + "_ch");
            File tmpFile = new File(tmpDir, DateUtil.getCurrentDate().getTime() + "_co_ch");

            BufferedReader reader = null;
            FileChannel balanceChannel = null;
            FileChannel rwChannel = null;
            FileChannel outChannel = null;
            try {
                //Charges file
                reader = getBufferedReader(chargesFile);

                dataSource.initContextDataSource(new ChargesContext(imessenger, mbOrganizationId, eircOrganizationId,
                        false, registry),
                        reader, chargesFile.fileName);

                //  Create a read-write charges containers memory-mapped file
                balanceChannel = new RandomAccessFile(chargeContainersFile, "rw").getChannel();
                ByteBuffer chargesBuffer = balanceChannel.map(FileChannel.MapMode.READ_WRITE, 0, chargesFile.getFileLength()*2);

                RegistryRecordData record;
                while ((record = dataSource.getNextRecord()) != null) {
                    int start = chargesBuffer.position();
                    record.writeContainers(chargesBuffer);
                    int end = chargesBuffer.position();
                    chargesContainers.put(getChargesContainerKey(record), new int[]{start, end});
                }
                IOUtils.closeQuietly(reader);

                //Corrections file
                reader = getBufferedReader(correctionsFile);
                dataSource.initContextDataSource(new CorrectionsContext(imessenger, mbOrganizationId, eircOrganizationId,
                        "ХАРЬКОВ", true, chargesContainers, chargesBuffer), reader, correctionsFile.getFileName());

                //  Create a read-write corrections memory-mapped file
                rwChannel = new RandomAccessFile(tmpFile, "rw").getChannel();
                ByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, correctionsFile.getFileLength()*2);

                registryFPFileFormat.writeRecordsAndFooter(dataSource, buffer);

                String eircFileName = registryFPFileFormat.fileName(registry);

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

                imessenger.addMessageInfo("total_lines", lineNum.get(), correctionsFile.getShortName(), eircFileName);

            } finally {
                IOUtils.closeQuietly(outChannel);
                IOUtils.closeQuietly(rwChannel);
                IOUtils.closeQuietly(balanceChannel);
                IOUtils.closeQuietly(reader);

                if (chargeContainersFile.exists()) {
                    chargeContainersFile.delete();
                }

                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }

		} catch (IOException e) {
			throw new MbParseException("Error reading file ", e);
		}

	}

    private String getChargesContainerKey(RegistryRecordData record) {
        return record.getPersonalAccountExt() + "_" + record.getServiceCode();
    }

    private void initRegistry(Registry registry) {
		registry.setCreationDate(DateUtil.getCurrentDate());
        registry.setRegistryNumber(DateUtil.getCurrentDate().getTime());
		registry.setType(RegistryType.SALDO_SIMPLE);
	}

	private void parseHeader(String[] fields, Registry registry, Context context) throws AbstractException {
        if (context.isSkipHeader()) {
            return;
        }
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

        int idx = 2;
        if (fields.length > 3) {
            try {
                registry.setRegistryNumber(Long.parseLong(fields[1] + fields[idx]));
            } catch (Exception e) {
                //
            }
            try {
                Date operationMonth = MbParsingConstants.OPERATION_MONTH_DATE_FORMAT.parseDateTime(fields[idx]).toDate();
                registry.setFromDate(operationMonth);
                registry.setTillDate(getLastDayOfMonth(operationMonth));
            } catch (Exception e) {
                //
            }
            idx++;
        }

		try {
			Date creationDate = MbParsingConstants.FILE_CREATION_DATE_FORMAT.parseDateTime(fields[idx]).toDate();
            registry.setCreationDate(creationDate);
		} catch (Exception e) {
            //
		}
	}

	private int parseRecord(String line, Queue<RegistryRecordData> recordStack, Context context) throws AbstractException {
		String[] fields = context.parseLine(line);

        if (fields == null) {
            return 0;
        }

		// remove duplicates in service codes
		Set<String> serviceCodes = ImmutableSet.<String>builder().add(context.getServiceCodes(fields).split("\\\\;")).build();

		int count = 0;
		for (String serviceCode : serviceCodes) {
			if (StringUtils.isEmpty(serviceCode) || "0".equals(serviceCode)) {
				return 0;
			}
			RegistryRecordData record = context.getRegistryRecord(fields, serviceCode);

            recordStack.add(record);
            count++;
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

    private Date getLastDayOfMonth(Date date) {
        return DateUtil.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(truncateMonth(date), 1), -1));
    }

    public Date truncateMonth(Date dt) {
        return DateUtils.truncate(dt, Calendar.MONTH);
    }

    private class CorrectionsContext extends Context {

        private String city;
        private Date fromDate;
        private Date tillDate;
        private Map<String, int[]> charges;
        private ByteBuffer chargesBuffer;

        private byte[] readBuffer = new byte[16*1024];

        private Cache<String, String> serviceCache = CacheBuilder.newBuilder().
                maximumSize(1000).
                expireAfterWrite(10, TimeUnit.MINUTES).
                build();

        public CorrectionsContext(IMessenger imessenger, Long mbOrganizationId, Long eircOrganizationId, String city,
                                  boolean skipHeader, Map<String, int[]> charges, ByteBuffer chargesBuffer) {
            super(imessenger, serviceCorrectionBean, mbOrganizationId,eircOrganizationId, skipHeader);
            this.city = city;
            this.charges = charges;
            this.chargesBuffer = chargesBuffer;
        }

        public Date getFromDate() {
            return fromDate;
        }

        public Date getTillDate() {
            return tillDate;
        }

        @Override
        public String[] parseLine(String line) {
            String[] fields = line.split(MbParsingConstants.DELIMITER);

            if (fields.length == MbParsingConstants.FIELDS_LENGTH_SKIP_RECORD) {
                log.debug("Skip record: {}", line);
                return null;
            }

            if (fields.length > MbParsingConstants.FIELDS_LENGTH_SKIP_RECORD &&
                    StringUtils.isEmpty(fields[9]) &&
                    StringUtils.isEmpty(fields[10]) &&
                    StringUtils.isEmpty(fields[19])) {
                fields = (String[]) ArrayUtils.remove(fields, 9);
                fields[9] = "-";
            }
/*
            if (StringUtils.isNotEmpty(fields[9])) {
                fields[9] = StringUtils.replace(fields[9], ";", "\\;");
            }*/
            return fields;
        }

        @Override
        public String getServiceCodes(String[] fields) {
            return fields[20];
        }

        @Override
        protected RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbParseException {
            return new CorrectionMapped(fields, serviceCode);
        }

        public void writeChargeContainers(ByteBuffer writeByteBuffer, RegistryRecordData recordData) {
            int[] idx = charges.get(getChargesContainerKey(recordData));
            if (idx == null) {
                return;
            }
            write(writeByteBuffer, FPRegistryConstants.CONTAINER_SEPARATOR);
            int length = idx[1] - idx[0];
            chargesBuffer.position(idx[0]);
            for (int i = 0; i < length; i++) {
                readBuffer[i] = chargesBuffer.get();
            }
            writeByteBuffer.put(readBuffer, 0, length);
        }

        private class CorrectionMapped extends RegistryRecordMapped {

            private String[] buildingFields;

            private CorrectionMapped(String[] fields, String serviceCode) throws MbParseException {
                super(fields, serviceCode);
            }

            public void initData(String[] fields, String serviceCode) throws MbParseException {
                super.initData(fields, serviceCode);

                Date modificationDate;
                try {
                    modificationDate = MbParsingConstants.CORRECTIONS_MODIFICATIONS_START_DATE_FORMAT.parseDateTime(fields[19]).toDate();
                    setModificationDate(modificationDate);
                } catch (Exception e) {
                    throw new MbParseException("Failed parse modification start date", e);
                }
                if (fromDate == null || modificationDate.before(fromDate)) {
                    fromDate = modificationDate;
                }
                if (tillDate == null || getModificationDate().after(tillDate)) {
                    tillDate = modificationDate;
                }
                buildingFields = parseBuildingAddress(fields[8]);
            }


            @Override
            public Long getId() {
                return null;
            }

            @Override
            public void setId(Long id) {

            }

            @Override
            public String getPersonalAccountExt() {
                return getField(1);
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
                return getField(2);
            }

            @Override
            public Date getOperationDate() {
                return getModificationDate();
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
                String operationDate = MbParsingConstants.OPERATION_DATE_FORMAT.print(getModificationDate().getTime());

                write(buffer, ContainerType.OPEN_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                write(buffer, ContainerType.EXTERNAL_ORGANIZATION_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, getField(0));
                write(buffer, ":");
                write(buffer, getMbOrganizationId());

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // ФИО
                write(buffer, ContainerType.SET_RESPONSIBLE_PERSON.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, getField(2));

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // Количество проживающих
                String containerValue = StringUtils.isEmpty(getField(15))? "0": getField(15);
                write(buffer, ContainerType.SET_NUMBER_ON_HABITANTS.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, containerValue);

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                // Отапливаемая площадь
                containerValue = StringUtils.isEmpty(getField(10))? "0.00": getField(10);
                write(buffer, ContainerType.SET_WARM_SQUARE.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, containerValue);

                writeChargeContainers(buffer, this);
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
                return getField(6);
            }

            @Override
            public String getStreetTypeCode() {
                return null;
            }

            @Override
            public String getStreet() {
                return getField(7);
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
                return getField(9);
            }
        }
    }

    private abstract class MbContextDataSource implements DataSource {
        private Context context;
        private BufferedReader reader;
        private String fileName;

        public void initContextDataSource(Context context, BufferedReader reader, String fileName) throws IOException {
            this.context = context;
            this.reader = reader;
            this.fileName = fileName;
        }

        public Context getContext() {
            return context;
        }

        public String readLine() throws IOException {
            return StringUtil.format(reader.readLine(), ParseRegistryConstants.DELIMITERS, ParseRegistryConstants.ESCAPE_SYMBOL);
        }

        public String getFileName() {
            return fileName;
        }
    }

    private class ChargesContext extends Context {

        private Registry registry;

        public ChargesContext(IMessenger imessenger, Long mbOrganizationId, Long eircOrganizationId, boolean skipHeader,
                              Registry registry) {
            super(imessenger, serviceCorrectionBean, mbOrganizationId, eircOrganizationId, skipHeader);
            this.registry = registry;
        }

        @Override
        public String[] parseLine(String line) {
            return line.split(MbParsingConstants.DELIMITER);
        }

        @Override
        public String getServiceCodes(String[] fields) {
            return fields[3];
        }

        @Override
        protected RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbParseException {
            return new ChargeMapped(fields, serviceCode);
        }

        private BigDecimal getMoney(String value) {
            return (StringUtils.isNotEmpty(value))? new BigDecimal(value).divide(new BigDecimal("100")):
                    new BigDecimal(0).divide(new BigDecimal("100"));
        }

        private class ChargeMapped extends RegistryRecordMapped {

            private ChargeMapped(String[] fields, String serviceCode) throws MbParseException {
                super(fields, serviceCode);
            }

            public void initData(String[] fields, String serviceCode) throws MbParseException {
                super.initData(fields, serviceCode);

                Date modificationDate;
                try {
                    modificationDate = MbParsingConstants.CHARGES_MODIFICATIONS_START_DATE_FORMAT.parseDateTime(fields[5]).toDate();
                    setModificationDate(modificationDate);
                } catch (Exception e) {
                    throw new MbParseException("Failed parse modification start date", e);
                }
            }

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public void setId(Long id) {

            }

            @Override
            public String getPersonalAccountExt() {
                return getField(4);
            }

            @Override
            public String getFirstName() {
                return null;
            }

            @Override
            public String getMiddleName() {
                return null;
            }

            @Override
            public String getLastName() {
                return null;
            }

            @Override
            public Date getOperationDate() {
                return null;
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
                if (!registry.getFromDate().equals(getModificationDate())) {
                    return;
                }

                String operationDate = MbParsingConstants.OPERATION_DATE_FORMAT.print(getModificationDate().getTime());

                BigDecimal charge = getMoney(getField(1));
                BigDecimal outgoingBalance = getMoney(getField(2));

                write(buffer, ContainerType.SALDO_OUT.getId());    // container type (SALDO OUT container) (1)
                write(buffer, ":");
                write(buffer, outgoingBalance.toString());      // outgoing balance (2)
                write(buffer, ":");
                write(buffer, operationDate);                   // operation date

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);

                write(buffer, ContainerType.CHARGE.getId());    // container type (CHARGE container) (1)
                write(buffer, ":");
                write(buffer, charge.toString());               // charge (2)
                write(buffer, ":");
                write(buffer, operationDate);                   // operation date
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
                return null;
            }

            @Override
            public String getStreetType() {
                return getField(6);
            }

            @Override
            public String getStreetTypeCode() {
                return null;
            }

            @Override
            public String getStreet() {
                return null;
            }

            @Override
            public String getBuildingNumber() {
                return null;
            }

            @Override
            public String getBuildingCorp() {
                return null;
            }

            @Override
            public String getApartment() {
                return null;
            }
        }
    }

    private enum MbFileType {
        CHARGES(".nac"), CORRECTIONS(".kor");


        private String extension;

        MbFileType(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    private class MbFile {
        private String fileName;
        private String shortName;
        private MbFileType fileType;
        private long fileLength;
        private InputStream inputStream;

        private MbFile(String dir, String fileName) throws FileNotFoundException {
            File mbFile = new File(dir, fileName);

            this.fileName = fileName;

            inputStream = new FileInputStream(mbFile);
            fileLength = mbFile.length();
            fileType = fileName.endsWith(MbFileType.CHARGES.getExtension()) ? MbFileType.CHARGES : MbFileType.CORRECTIONS;
            shortName = StringUtils.removeEnd(fileName, fileType.getExtension());
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileLength() {
            return fileLength;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public MbFileType getFileType() {
            return fileType;
        }

        public String getShortName() {
            return shortName;
        }
    }
}
