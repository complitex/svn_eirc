package ru.flexpay.eirc.mb_transformer.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.PatternFilenameFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.wicket.util.io.IOUtils;
import org.complitex.correction.entity.LinkStatus;
import org.complitex.correction.entity.OrganizationCorrection;
import org.complitex.correction.service.OrganizationCorrectionBean;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.SqlSessionFactoryBean;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.DateUtil;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.dictionary.entity.Address;
import ru.flexpay.eirc.dictionary.entity.Person;
import ru.flexpay.eirc.mb_transformer.entity.Context;
import ru.flexpay.eirc.mb_transformer.entity.MbFile;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;
import ru.flexpay.eirc.mb_transformer.entity.RegistryRecordMapped;
import ru.flexpay.eirc.mb_transformer.util.MbParsingConstants;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.service.file.RegistryFPFileService;
import ru.flexpay.eirc.registry.service.handle.MbConverterQueueProcessor;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;
import ru.flexpay.eirc.registry.util.FPRegistryConstants;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.correction.service.ServiceCorrectionBean;
import ru.flexpay.eirc.service.service.ServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MbCorrectionsFileConverter {

    private static final Logger log = LoggerFactory.getLogger(MbCorrectionsFileConverter.class);

    @EJB
    private OrganizationCorrectionBean organizationCorrectionBean;

    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB(name = "MbTransformerConfigBean")
    private MbTransformerConfigBean configBean;

    @EJB
    private RegistryFPFileService registryFPFileService;

    @EJB
    private MbConverterQueueProcessor mbConverterQueueProcessor;

    @EJB
    private MbTransformerRegistryBean mbTransformerRegistryBean;

    private static final String REGISTRY_NUMBER = "registry_number";

    public void init2() {
        SqlSessionFactoryBean sqlSessionFactoryBean = configBean == null ? new SqlSessionFactoryBean() :
                new SqlSessionFactoryBean() {
                    @Override
                    public SqlSessionManager getSqlSessionManager() {
                        return getSqlSessionManager(configBean.getString(MbTransformerConfig.EIRC_DATA_SOURCE), "remote");
                    }
                };
        organizationCorrectionBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);

        serviceCorrectionBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);

        serviceBean.setSqlSessionFactoryBean(sqlSessionFactoryBean);
    }

    /**
     * Using in console implementation
     */
    public void init() {
        organizationCorrectionBean = new OrganizationCorrectionBean();

        serviceCorrectionBean = new ServiceCorrectionBean();

        serviceBean = new ServiceBean();

        registryFPFileService = new RegistryFPFileService();

        init2();
    }

    /**
     * Use only with glassfish
     *
     * @param imessenger
     * @param finishConvert
     * @throws ExecutionException
     */
    public void convert(final IMessenger imessenger, final FinishCallback finishConvert) throws ExecutionException {
        imessenger.addMessageInfo("mb_registry_convert_starting");
        finishConvert.init();

        mbConverterQueueProcessor.execute(
                new AbstractJob<Void>() {
                    @Override
                    public Void execute() throws ExecuteException {
                        try {

                            final String dir = configBean.getString(MbTransformerConfig.WORK_DIR, true);
                            final String tmpDir = configBean.getString(MbTransformerConfig.TMP_DIR, true);
                            final Long mbOrganizationId = configBean.getInteger(MbTransformerConfig.MB_ORGANIZATION_ID, true).longValue();
                            final Long eircOrganizationId = configBean.getInteger(MbTransformerConfig.EIRC_ORGANIZATION_ID, true).longValue();

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
                                            convertFile(correctionsFiles.get(fileEntry.getKey()), fileEntry.getValue(),
                                                    dir, null, tmpDir, mbOrganizationId, eircOrganizationId, imessenger);
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

    private BufferedReader getBufferedReader(MbFile mbFile) throws MbConverterException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(mbFile.getInputStream(), MbParsingConstants.REGISTRY_FILE_CHARSET), (int)mbFile.getFileLength());
        } catch (Exception e) {
            throw new MbConverterException("Error open file " + mbFile.getFileName(), e);
        }
        return reader;
    }

    public void convertFile(final MbFile correctionsFile, final MbFile chargesFile, final String dir, final String eircFileName,
                            final String tmpDir, final Long mbOrganizationId, final Long eircOrganizationId,
                            final AbstractMessenger imessenger, final AbstractFinishCallback finishConvert) throws AbstractException {
        imessenger.addMessageInfo("mb_registry_convert_starting");
        finishConvert.init();

        mbConverterQueueProcessor.execute(
            new AbstractJob<Void>() {
                @Override
                public Void execute() throws ExecuteException {
                    try {
                        convertFile(correctionsFile, chargesFile, dir, eircFileName, tmpDir, mbOrganizationId,
                                eircOrganizationId, imessenger);
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

    /**
     * Can be using in console application. Before starting execute init() method
     *
     * @param correctionsFile
     * @param chargesFile
     * @param dir
     * @param eircFileName
     * @param tmpDir
     * @param mbOrganizationId
     * @param eircOrganizationId
     * @param imessenger
     * @throws AbstractException
     */
    @SuppressWarnings ({"unchecked"})
	public void convertFile(final MbFile correctionsFile, final MbFile chargesFile, String dir, String eircFileName,
                            final String tmpDir, Long mbOrganizationId, Long eircOrganizationId,
                            final AbstractMessenger imessenger) throws AbstractException {

        final Registry registry = new Registry();
        initRegistry(registry);

        final AtomicInteger lineNum = new AtomicInteger(0);
        final AtomicInteger recordNum = new AtomicInteger(0);

        final Map<String, int[]> chargesContainers = Maps.newHashMapWithExpectedSize(((int)chargesFile.getFileLength())/30);
        final Map<String, List<String>> chargesServices = Maps.newHashMapWithExpectedSize(((int)chargesFile.getFileLength())/30);

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

            File chargeContainersFile = new File(tmpDir, DateUtil.getCurrentDate().getTime() + "_ch");
            File tmpFile = new File(tmpDir, DateUtil.getCurrentDate().getTime() + "_co_ch");

            BufferedReader reader = null;
            FileChannel balanceChannel = null;
            FileChannel rwChannel = null;
            FileChannel outChannel = null;
            try {
                String eircDataSource = getDataSource();

                //Charges file
                reader = getBufferedReader(chargesFile);

                dataSource.initContextDataSource(new ChargesContext(imessenger, eircDataSource, mbOrganizationId, eircOrganizationId,
                        false, registry),
                        reader, chargesFile.getFileName());

                //  Create a read-write charges containers memory-mapped file
                balanceChannel = new RandomAccessFile(chargeContainersFile, "rw").getChannel();
                ByteBuffer chargesBuffer = balanceChannel.map(FileChannel.MapMode.READ_WRITE, 0, chargesFile.getFileLength()*2);

                RegistryRecordData record;
                while ((record = dataSource.getNextRecord()) != null) {
                    // containers
                    int start = chargesBuffer.position();
                    record.writeContainers(chargesBuffer);
                    int end = chargesBuffer.position();
                    chargesContainers.put(getChargesContainerKey(record), new int[]{start, end});

                    // services
                    if (chargesServices.containsKey(record.getPersonalAccountExt())) {
                        chargesServices.get(record.getPersonalAccountExt()).add(record.getServiceCode());
                    } else {
                        chargesServices.put(record.getPersonalAccountExt(), Lists.newArrayList(record.getServiceCode()));
                    }
                }
                IOUtils.closeQuietly(reader);

                //Corrections file
                reader = getBufferedReader(correctionsFile);
                dataSource.initContextDataSource(new CorrectionsContext(imessenger, eircDataSource, mbOrganizationId, eircOrganizationId,
                        "ХАРЬКОВ", true, registry, chargesContainers, chargesServices, chargesBuffer), reader, correctionsFile.getFileName());

                //  Create a read-write corrections memory-mapped file
                rwChannel = new RandomAccessFile(tmpFile, "rw").getChannel();
                ByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, correctionsFile.getFileLength()*2);

                registryFPFileService.writeRecordsAndFooter(dataSource, buffer);

                chargesBuffer.clear();

                if (eircFileName == null) {
                    eircFileName = registryFPFileService.fileName(registry);
                }

                // Create registry file
                outChannel = new FileOutputStream(new File(dir, eircFileName)).getChannel();
                ByteBuffer buff = ByteBuffer.allocateDirect(32 * 1024);

                registryFPFileService.writeHeader(dataSource, buff);

                buff.flip();
                outChannel.write(buff);
                buff.clear();

                buffer.flip();
                outChannel.write(buffer);
                buffer.clear();

                if (dataSource.getContext().isValid()) {
                    imessenger.addMessageInfo("eirc_saldo_simple_created",
                            chargesFile.getFileName(), correctionsFile.getFileName(), eircFileName, registry.getRecordsCount());
                } else {
                    imessenger.addMessageError("eirc_saldo_simple_created",
                            chargesFile.getFileName(), correctionsFile.getFileName(), eircFileName, registry.getRecordsCount());
                }

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
			throw new MbConverterException("Error reading file ", e);
		}

	}

    private String getChargesContainerKey(RegistryRecordData record) {
        return record.getPersonalAccountExt() + "_" + record.getServiceCode();
    }

    private void initRegistry(Registry registry) {
		registry.setCreationDate(DateUtil.getCurrentDate());
        registry.setRegistryNumber(mbTransformerRegistryBean.generateRegistryNumber());
		registry.setType(RegistryType.SALDO_SIMPLE);
	}

	private void parseHeader(String[] fields, Registry registry, Context context) throws AbstractException {
        if (context.isSkipHeader()) {
            return;
        }
		log.debug("fields: {}", fields);
		log.debug("Getting service provider with id = {} from DB", fields[1]);
        FilterWrapper<OrganizationCorrection> filter = FilterWrapper.of(new OrganizationCorrection(null, null, "[0]*" + fields[1],
                context.getMbOrganizationId(), context.getEircOrganizationId(), null));
        filter.setRegexp(true);
		List<OrganizationCorrection> organizationCorrections = organizationCorrectionBean.getOrganizationCorrections(getDataSource(),
                filter);
		if (organizationCorrections.size() <= 0) {
			throw new MbConverterException("No service provider correction with id {0}", fields[1]);
		}
        if (organizationCorrections.size() > 1) {
            throw new MbConverterException("Found several correction for service provider {0}", fields[1]);
        }
        Long serviceProviderId = organizationCorrections.get(0).getObjectId();

		registry.setSenderOrganizationId(serviceProviderId);
		registry.setRecipientOrganizationId(context.getEircOrganizationId());

        int idx = 2;
        if (fields.length > 3) {
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
        private Map<String, List<String>> services;
        private ByteBuffer chargesBuffer;
        private boolean valid = true;

        private byte[] readBuffer = new byte[16*1024];

        public CorrectionsContext(AbstractMessenger imessenger, String dataSource, Long mbOrganizationId, Long eircOrganizationId,
                                  String city, boolean skipHeader, Registry registry,
                                  Map<String, int[]> charges, Map<String, List<String>> services, ByteBuffer chargesBuffer) {
            super(imessenger, serviceCorrectionBean, serviceBean, dataSource, mbOrganizationId,eircOrganizationId, skipHeader, registry);
            this.city = city;
            this.charges = charges;
            this.services = services;
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
            String serviceCodes = fields[20];
            if (StringUtils.isEmpty(serviceCodes) || "0".equals(serviceCodes)) {
                String personalAccountExt = fields[1];
                List<String> currentServices = services.get(personalAccountExt);
                if (currentServices != null && currentServices.size() > 0) {
                    return StringUtils.join(currentServices, ';');
                }
            }
            return serviceCodes;
        }

        @Override
        protected RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbConverterException {
            return new CorrectionMapped(fields, serviceCode);
        }

        public boolean writeChargeContainers(ByteBuffer writeByteBuffer, RegistryRecordData recordData) {
            int[] idx = charges.remove(getChargesContainerKey(recordData));
            if (idx == null) {
                getIMessenger().addMessageError("mb_registries_fail_not_charges", recordData.getPersonalAccountExt(), recordData.getServiceCode());
                log.error("Can not find account {} in MB charges (service code - {})", recordData.getPersonalAccountExt(), recordData.getServiceCode());
                return false;
            }
            write(writeByteBuffer, FPRegistryConstants.CONTAINER_SEPARATOR);
            int length = idx[1] - idx[0];
            chargesBuffer.position(idx[0]);
            for (int i = 0; i < length; i++) {
                readBuffer[i] = chargesBuffer.get();
            }
            writeByteBuffer.put(readBuffer, 0, length);
            return true;
        }

        @Override
        public boolean isValid() {
            if (charges != null && charges.size() > 0) {
                valid = false;
                for (String key : charges.keySet()) {
                    String[] account = StringUtils.split(key, "_", 2);
                    getIMessenger().addMessageError("mb_registries_fail_not_correction", account);
                    log.error("Can not find account {} in MB corrections (service code - {})", account);
                }
            }
            return valid;
        }

        private class CorrectionMapped extends RegistryRecordMapped {

            private String[] buildingFields;
            private String[] nameFields;

            private CorrectionMapped(String[] fields, String serviceCode) throws MbConverterException {
                super(fields, serviceCode);
            }

            public void initData(String[] fields, String serviceCode) throws MbConverterException {
                super.initData(fields, serviceCode);

                Date modificationDate;
                try {
                    modificationDate = MbParsingConstants.CORRECTIONS_MODIFICATIONS_START_DATE_FORMAT.parseDateTime(fields[19]).toDate();
                    setModificationDate(modificationDate);
                } catch (Exception e) {
                    throw new MbConverterException("Failed parse modification start date", e);
                }
                if (fromDate == null || modificationDate.before(fromDate)) {
                    fromDate = modificationDate;
                }
                if (tillDate == null || getModificationDate().after(tillDate)) {
                    tillDate = modificationDate;
                }
                buildingFields = parseBuildingAddress(fields[8]);
                nameFields = StringUtils.split(fields[2], " ", 3);
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
                return ArrayUtils.getLength(nameFields) > 1? nameFields[1] : "";
            }

            @Override
            public String getMiddleName() {
                return ArrayUtils.getLength(nameFields) > 2? nameFields[2] : "";
            }

            @Override
            public String getLastName() {
                return ArrayUtils.getLength(nameFields) > 0? nameFields[0] : "";
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
            public Long getRoomId() {
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
                /*
                write(buffer, ContainerType.OPEN_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");

                write(buffer, FPRegistryConstants.CONTAINER_SEPARATOR);
                */
                write(buffer, ContainerType.EXTERNAL_ORGANIZATION_ACCOUNT.getId());
                write(buffer, ":");
                write(buffer, operationDate);
                write(buffer, "::");
                write(buffer, getField(0));
                write(buffer, ":");
                write(buffer, getMbOrganizationId());

                /*
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
                */
                valid &= writeChargeContainers(buffer, this);
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
            public void setRoomId(Long id) {

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

            @Override
            public String getRoom() {
                return null;
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

        public ChargesContext(AbstractMessenger imessenger, String dataSource, Long mbOrganizationId, Long eircOrganizationId, boolean skipHeader,
                              Registry registry) {
            super(imessenger, serviceCorrectionBean, serviceBean, dataSource, mbOrganizationId, eircOrganizationId, skipHeader, registry);
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
        protected RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbConverterException {
            return new ChargeMapped(fields, serviceCode);
        }

        private BigDecimal getMoney(String value) {
            return (StringUtils.isNotEmpty(value))? new BigDecimal(value).divide(new BigDecimal("100")):
                    new BigDecimal(0).divide(new BigDecimal("100"));
        }

        private class ChargeMapped extends RegistryRecordMapped {

            private ChargeMapped(String[] fields, String serviceCode) throws MbConverterException {
                super(fields, serviceCode);
            }

            public void initData(String[] fields, String serviceCode) throws MbConverterException {
                super.initData(fields, serviceCode);

                Date modificationDate;
                try {
                    modificationDate = MbParsingConstants.CHARGES_MODIFICATIONS_START_DATE_FORMAT.parseDateTime(fields[5]).toDate();
                    setModificationDate(modificationDate);
                } catch (Exception e) {
                    throw new MbConverterException("Failed parse modification start date", e);
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
            public Long getRoomId() {
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
                if (!getRegistry().getFromDate().equals(getModificationDate())) {
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
            public void setRoomId(Long id) {

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

            @Override
            public String getRoom() {
                return null;
            }
        }
    }

    private String getDataSource() {
        return configBean.getString(MbTransformerConfig.EIRC_DATA_SOURCE);
    }

}
