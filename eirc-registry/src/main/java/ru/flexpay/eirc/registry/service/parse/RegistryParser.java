package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.complitex.dictionary.entity.DictionaryConfig;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.mybatis.Transactional;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.DateUtil;
import org.complitex.dictionary.util.EjbBeanLocator;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.service.ServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Sknar
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateless
public class RegistryParser implements Serializable {

    private final Logger log = LoggerFactory.getLogger(RegistryParser.class);

    @EJB
    private RegistryRecordBean registryRecordService;

    @EJB
    private RegistryBean registryService;
    @EJB
    private ServiceBean serviceBean;

    @EJB
    private RegistryWorkflowManager registryWorkflowManager;
    @EJB
    private RegistryRecordWorkflowManager recordWorkflowManager;

    @EJB
    private EircOrganizationStrategy organizationStrategy;

    @EJB
    private ConfigBean configBean;

    @EJB
    private JobProcessor processor;

    @EJB
    private ParserQueueProcessor parserQueueProcessor;

    //private AtomicInteger inc = new AtomicInteger(0);

    public void parse(final IMessenger imessenger, final FinishCallback finishUpload) throws ExecuteException {
        imessenger.addMessageInfo("starting_upload_registries");
        finishUpload.init();

        final String dir = configBean.getString(DictionaryConfig.IMPORT_FILE_STORAGE_DIR, true);

        String[] fileNames = new File(dir).list();

        if (fileNames == null || fileNames.length == 0) {
            imessenger.addMessageInfo("files_not_found");
            finishUpload.complete();
            return;
        }
        final AtomicInteger recordCounter = new AtomicInteger(fileNames.length);

        for (final String fileName : fileNames) {
            parserQueueProcessor.execute(new AbstractJob<Void>() {
                @Override
                public Void execute() throws ExecuteException {

                    try {
                        InputStream is = new FileInputStream(new File(dir, fileName));
                        Registry registry = EjbBeanLocator.getBean(RegistryParser.class).parse(imessenger, is, fileName);

                        if (registry != null) {
                            imessenger.addMessageInfo("registry_created", registry.getRegistryNumber(), fileName);
                        } else {
                            imessenger.addMessageError("registry_failed_upload", fileName);
                        }

                        return null;
                    } catch (Throwable th) {
                        imessenger.addMessageError("registry_failed_upload", fileName);
                        throw new ExecuteException(th, "Failed upload registry file by file name: " + fileName);
                    } finally {
                        if (recordCounter.decrementAndGet() == 0) {
                            imessenger.addMessageInfo("registry_finish_upload");
                            finishUpload.complete();
                        }
                    }
                }
            });
        }
    }

    public Registry parse(IMessenger imessenger, InputStream is, String fileName) throws ExecuteException {
        int numberReadChars = configBean.getInteger(RegistryConfig.NUMBER_READ_CHARS, true);
        int numberFlushRegistryRecords = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
        return parse(imessenger, is, fileName, numberReadChars, numberFlushRegistryRecords);
    }

    public Registry parse(IMessenger imessenger, InputStream is, String fileName, int numberReadChars, int numberFlushRegistryRecords)
            throws ExecuteException {
        log.debug("start action");

        Logger processLog = getProcessLogger();

        Context context = new Context(imessenger, numberFlushRegistryRecords);

        FileReader reader = new FileReader(is);

        try {
            List<FileReader.Message> listMessage = Lists.newArrayList();
            boolean nextIterate;
            Registry registry = null;
            do {
                listMessage = reader.getMessages(listMessage, numberReadChars);
                log.debug("read {} number records", listMessage.size());

                for (FileReader.Message message : listMessage) {
                    if (message == null) {
                        if (context.getRegistry() == null) {
                            imessenger.addMessageError("file_is_not_registry", fileName);
                        }
                        finalizeRegistry(context, processLog);
                        return registry;
                    }
                    String messageValue = message.getBody();
                    if (StringUtils.isEmpty(messageValue)) {
                        continue;
                    }
                    List<String> messageFieldList = StringUtil.splitEscapable(
                            messageValue, ParseRegistryConstants.RECORD_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);

                    Integer messageType = message.getType();

                    if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_HEADER)) {
                        registry = processHeader(imessenger, fileName, messageFieldList, processLog);
                        if (registry == null) {
                            return null;
                        }
                        saveRegistry(registry);

                        processLog = getProcessLogger(registry.getRegistryNumber());

                        context.setRegistry(registry);
                        context.addMessageInfo("registry_creating", registry.getRegistryNumber(), fileName);
                        log.debug("Creating registry {}", registry.getId());
                    } else if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_RECORD)) {
                        RegistryRecordData record = processRecord(registry, messageFieldList, processLog);
                        if (record == null) {
                            return null;
                        }
                        context.add(record);
                        flushRecordStack(context, processLog);
                    } else if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_FOOTER)) {
                        processFooter(messageFieldList, context, processLog);
                    }
                }
                nextIterate = !listMessage.isEmpty();
                listMessage.clear();

            } while(nextIterate);
        } catch(Exception e) {
            log.error("Processing error", e);
            processLog.error("Inner error");
            setErrorStatus(context.getRegistry());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Failed reader", e);
                processLog.error("Inner error");
            }
            EjbBeanLocator.getBean(RegistryParser.class).setLoadedStatus(context);
        }

        return context.getRegistry();
    }

    private Logger getProcessLogger() {
        return getProcessLogger(-1L);
    }

    private Logger getProcessLogger(Long registryId) {
        InvocationHandler handler = new RegistryLogger(log, registryId);
        ClassLoader cl = Logger.class.getClassLoader();
        return (Logger) Proxy.newProxyInstance(cl, new Class[]{Logger.class}, handler);
    }

    private void flushRecordStack(Context context, Logger processLog) throws ExecuteException {
        flushRecordStack(context, false, processLog);
    }

    private void flushRecordStack(final Context context, boolean finalize, Logger processLog) throws ExecuteException {
        if (context.getRecords() != null &&
                (context.getRecords().size() >= context.getNumberFlushRegistryRecords() || finalize)) {

            final List<RegistryRecordData> records = context.getRecords();

            context.getBatchProcessor().processJob(new AbstractJob<JobResult>() {
                @Override
                public JobResult execute() throws ExecuteException {
                    /*
                    long i = context.getNumberFlushRegistryRecords()*inc.incrementAndGet();
                    for (RegistryRecordData record : records) {
                        ((RegistryRecord)record).setUniqueOperationNumber(i);
                        ((RegistryRecord)record).setContainers(Lists.newArrayList(new Container(String.valueOf(i), ContainerType.BASE)));
                        i++;
                    }*/
                    registryRecordService.createBulk(records);
                    //TODO save intermediate state
                    int currentCounter = context.addRecordCounter(records.size());
                    context.addMessageInfo("registry_record_upload", currentCounter, context.getRegistry().getRegistryNumber());
                    return JobResult.SUCCESSFUL;
                }
            });

            context.clearRecords();
        }
    }

    private void finalizeRegistry(Context context, Logger processLog) throws ExecuteException {

        if (context.getRegistry() == null) {
            return;
        }

        flushRecordStack(context, true, processLog);

        context.getBatchProcessor().waitEndWorks();

        log.debug("Finalize registry");

        boolean failed = false;

        if (context.getRegistry().getRecordsCount() != context.getRecordCounter()) {
            context.addMessageError("registry_records_number_error", context.getRegistry().getRegistryNumber(),
                    context.getRecordCounter(), context.getRegistry().getRecordsCount());
            processLog.error("Registry records number error, expected: {}, found: {}",
                    new Object[]{context.getRegistry().getRecordsCount(), context.getRecordCounter()});
            failed = true;
        }

        if (!context.checkTotalAmount()) {
            context.addMessageError("total_amount_error", context.getRegistry().getRegistryNumber(),
                    context.getRegistry().getAmount(), context.getTotalAmount());
            processLog.error("Total amount error, expected: {}, found: {}",
                    new Object[]{context.getRegistry().getAmount(), context.getTotalAmount()});
            failed = true;
        }

        if (failed) {
            setNextErrorStatus(context);
        } else {
            setLoadedStatus(context);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setLoadedStatus(Context context) throws ExecuteException {
        if (context.getRegistry() == null || registryWorkflowManager.isLoaded(context.getRegistry())) {
            return;
        }
        try {
            registryWorkflowManager.setNextSuccessStatus(context.getRegistry());
        } catch (TransitionNotAllowed transitionNotAllowed) {
            throw new ExecuteException("Does not finalize registry", transitionNotAllowed);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setNextErrorStatus(Context context) throws ExecuteException {
        if (context.getRegistry() == null) {
            return;
        }
        try {
            registryWorkflowManager.setNextErrorStatus(context.getRegistry());
        } catch (TransitionNotAllowed transitionNotAllowed) {
            throw new ExecuteException("Does not finalize registry", transitionNotAllowed);
        }
    }

    public boolean setErrorStatus(Registry registry) {
        if (registry == null) {
            return false;
        }
        try {
            registryWorkflowManager.markLoadingHasError(registry);
            return true;
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Can not set error status. Current status: " + transitionNotAllowed.getType(), transitionNotAllowed);
        }
        return false;
    }

    private Registry processHeader(IMessenger iMessenger, String fileName, List<String> messageFieldList, Logger processLog) {
        if (messageFieldList.size() < 10) {
            processLog.error("Message header error, invalid number of fields: {}, expected at least 10", messageFieldList.size());
            return null;
        }

        processLog.info("Adding header: {}", messageFieldList);

        DateTimeFormatter dateFormat = ParseRegistryConstants.HEADER_DATE_FORMAT;

        final Registry newRegistry = new Registry();
        try {
            registryWorkflowManager.setInitialStatus(newRegistry);
            // TODO attach file
            //newRegistry.getFiles().put(registryFPFileTypeService.findByCode(RegistryFPFileType.MB_FORMAT), spFile);
            int n = 0;
            newRegistry.setRegistryNumber(Long.valueOf(messageFieldList.get(++n)));
            String value = messageFieldList.get(++n);

            Long registryTypeId = Long.valueOf(value);
            RegistryType registryType = null;
            for (RegistryType item : RegistryType.values()) {
                if (item.getId().equals(registryTypeId)) {
                    registryType = item;
                    break;
                }
            }
            if (registryType == null) {
                iMessenger.addMessageError("unknown_registry_type", fileName, value);
                processLog.error("Unknown registry type field: {}", value);
                return null;
            }
            newRegistry.setType(registryType);
            newRegistry.setRecordsCount(Integer.valueOf(messageFieldList.get(++n)));
            newRegistry.setCreationDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
            newRegistry.setFromDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
            newRegistry.setTillDate(dateFormat.parseDateTime(messageFieldList.get(++n)).toDate());
            newRegistry.setSenderOrganizationId(Long.valueOf(messageFieldList.get(++n)));
            newRegistry.setRecipientOrganizationId(Long.valueOf(messageFieldList.get(++n)));
            String amountStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(amountStr)) {
                newRegistry.setAmount(new BigDecimal(amountStr));
            }
            if (messageFieldList.size() > n + 1) {
                if (!parseContainers(newRegistry.getContainers(), messageFieldList.get(++n), processLog)) {
                    return null;
                }
            }

            processLog.info("Creating new registry: {}", newRegistry);

            Organization recipient = getRecipient(newRegistry, iMessenger, processLog);
            if (recipient == null) {
                iMessenger.addMessageError("recipient_not_found", newRegistry.getRegistryNumber());
                processLog.error("Failed processing registry header, recipient not found: #{}", newRegistry.getRecipientOrganizationId());
                return null;
            }
            Organization sender = getSender(newRegistry, processLog);
            if (sender == null) {
                iMessenger.addMessageError("sender_not_found", newRegistry.getRegistryNumber());
                processLog.error("Failed processing registry header, sender not found: #{}", newRegistry.getSenderOrganizationId());
                return null;
            }
            processLog.info("Recipient: {}\n sender: {}", recipient, sender);

            if (!validateServiceProvider(newRegistry, iMessenger, processLog)) {
                return null;
            }

            if (!validateRegistry(newRegistry, iMessenger, processLog)) {
                return null;
            }

            newRegistry.setLoadDate(DateUtil.getCurrentDate());

            return newRegistry;
        } catch (Exception e) {
            iMessenger.addMessageError("header_parse_error", fileName, e.getMessage());
            processLog.error("Header parse error in file: {}", fileName, e);
        }
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void saveRegistry(Registry registry) {
        registryService.save(registry);

            /*
            final CountDownLatch latch = new CountDownLatch(1);
            processor.processJob(new AbstractJob<Void>() {
                @Override
                public Void execute() throws ExecuteException {
                    try {
                        EjbBeanLocator.getBean(RegistryBean.class).save(newRegistry);
                        return null;
                    } finally {
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                //
            }
            */
    }

    private boolean validateServiceProvider(Registry registry, IMessenger iMessenger, Logger processLog) {
        Organization provider = getProvider(registry);
        /*
        if (registry.getType().isPayments()) {
            Stub<Organization> recipient = new Stub<Organization>(registry.getRecipientOrganizationId());
            if (recipient.sameId(ApplicationConfig.getSelfOrganization())) {
                processLog.error("Expected service provider recipient, but recieved eirc code");
                return false;
            }
        }
        */
        if (!provider.isServiceProvider()) {
            iMessenger.addMessageError("organization_not_service_provider", provider.getId(), registry.getRegistryNumber());
            processLog.error("Organization found, but it is not service provider: {}", provider);
            return false;
        }
        return true;
    }

    private Organization getProvider(Registry registry) {
        // for payments registry assume recipient is a service provider

        Long providerId = registry.getType().isPayments()? registry.getRecipientOrganizationId() : registry.getSenderOrganizationId();

        return organizationStrategy.findById(providerId, false);
    }

    private Organization getSender(Registry registry, Logger processLog) {

        processLog.debug("Fetching sender via code={}", registry.getSenderOrganizationId());
        Organization sender = findOrgByRegistryCorrections(registry, registry.getSenderOrganizationId(), processLog);
        if (sender == null) {
            sender = organizationStrategy.findById(registry.getSenderOrganizationId(), false);
        }
        return sender;
    }

    private Organization getRecipient(Registry registry, IMessenger iMessenger, Logger processLog) {

        Integer eircOrganizationId = configBean.getInteger(RegistryConfig.SELF_ORGANIZATION_ID, true);

        if (eircOrganizationId == null) {
            iMessenger.addMessageError("eirc_organization_id_not_defined");
            processLog.error("Eirc organization is not defined in config");
            return null;
        }

        if (registry.getRecipientOrganizationId() == null || registry.getRecipientOrganizationId() == 0) {
            processLog.debug("Recipient is EIRC, code=0");
            configBean.getConfigs();
            return organizationStrategy.findById(eircOrganizationId, false);
        }

        if (!registry.getRecipientOrganizationId().equals(eircOrganizationId.longValue())) {
            iMessenger.addMessageError("recipient_not_eirc_organization", registry.getRegistryNumber());
            processLog.error("Recipient is not EIRC organization");
            return null;
        }

        processLog.debug("Fetching recipient via code={}", registry.getRecipientOrganizationId());
        Organization recipient = findOrgByRegistryCorrections(registry, registry.getRecipientOrganizationId(), processLog);
        if (recipient == null) {
            recipient = organizationStrategy.findById(registry.getRecipientOrganizationId(), false);
        }
        return recipient;
    }

    // TODO Find organization in corrections
    private Organization findOrgByRegistryCorrections(Registry registry, Long code, Logger processLog) {
        /*
        for (Container container : registry.getContainers()) {
            String data = container.getData();
            processLog.debug("Candidate: {}", data);
            if (data.startsWith("502"+ ParseRegistryConstants.CONTAINER_DATA_DELIMITER)) {
                List<String> datum = StringUtil.splitEscapable(
                        data, ParseRegistryConstants.CONTAINER_DATA_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
                // skip if correction is not for Organization type
                if (Integer.parseInt(datum.get(1)) != typeRegistry.getType(Organization.class)) {
                    continue;
                }
                // skip if correction is not for the object with requested code
                if (Long.parseLong(datum.get(2)) != code) {
                    continue;
                }

                if (StringUtils.isNotBlank(datum.get(4)) && "1".equals(datum.get(5))) {
                    Stub<Organization> stub = correctionsService.findCorrection(
                            datum.get(4), Organization.class, masterIndexService.getMasterSourceDescriptionStub());
                    if (stub == null) {
                        throw new IllegalStateException("Expected master correction for organization, " +
                                "but not found: " + data);
                    }
                    processLog.debug("Found organization by master correction: {}", datum.get(4));
                    Organization org = organizationStrategy.readFull(stub);
                    if (org == null) {
                        throw new IllegalStateException("Existing master correction for organization " +
                                "references nowhere: " + data);
                    }
                    return org;
                }
            }
        }*/

        return null;
    }

    private boolean parseContainers(List<Container> distContainers, String containersData, Logger processLog) {

        List<String> containers = StringUtil.splitEscapable(
                containersData, ParseRegistryConstants.CONTAINER_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
        for (String data : containers) {
            if (StringUtils.isBlank(data)) {
                continue;
            }
            if (data.length() > ParseRegistryConstants.MAX_CONTAINER_SIZE) {
                processLog.error("Too long container found: {}", data);
                return false;
            }
            List<String> containerData = StringUtil.splitEscapable(
                    data, ParseRegistryConstants.CONTAINER_DATA_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
            if (containerData.size() < 1) {
                processLog.error("Failed container format: {}", containerData);
                return false;
            }

            ContainerType containerType = ContainerType.valueOf(Long.parseLong(containerData.get(0)));

            distContainers.add(new Container(data, containerType));
        }
        return true;
    }

    @Transactional(isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED)
    public boolean validateRegistry(Registry registry, IMessenger iMessenger, Logger processLog) {
        Registry filterObject = new Registry();
        filterObject.setRegistryNumber(registry.getRegistryNumber());
        int countRegistries = registryService.count(FilterWrapper.of(filterObject));
        if (countRegistries > 0) {
            iMessenger.addMessageError("registry_was_already_uploaded", registry.getRegistryNumber());
            processLog.error("Registry was already uploaded", registry.getRegistryNumber());
            return false;
        }
        return true;
    }

    private RegistryRecordData processRecord(Registry registry, List<String> messageFieldList, Logger processLog) {
        if (messageFieldList.size() < 10) {
            processLog.error("Message record error, invalid number of fields: {}", messageFieldList.size());
            return null;
        }

        boolean failed = false;
        RegistryRecord record = new RegistryRecord();
        record.setRegistryId(registry.getId());
        try {
            int n = 1;
            record.setServiceCode(messageFieldList.get(++n));
            record.setPersonalAccountExt(messageFieldList.get(++n));

            //TODO find by external id, if service code started by # (maybe using correction)
            /*
            FilterWrapper<Service> filter = FilterWrapper.of(new Service(record.getServiceCode()));
            filter.setSortProperty(null);
            List<Service> services = serviceBean.getServices(filter);
            if (services.size() == 0) {
                processLog.warn("Not found service by code {}", record.getServiceCode());
            }
            */

            // setup consumer address
            String addressStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(addressStr)) {
                List<String> addressFieldList = StringUtil.splitEscapable(
                        addressStr, ParseRegistryConstants.ADDRESS_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);

                if (addressFieldList.size() != 6) {
                    throw new RegistryFormatException(
                            String.format("Address group '%s' has invalid number of fields %d",
                                    addressStr, addressFieldList.size()));
                }
                record.setCity(addressFieldList.get(0));
                record.setStreetType(addressFieldList.get(1));
                record.setStreet(addressFieldList.get(2));
                record.setBuildingNumber(addressFieldList.get(3));
                record.setBuildingCorp(addressFieldList.get(4));
                record.setApartment(addressFieldList.get(5));
            }

            // setup person first, middle, last names
            String fioStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(fioStr)) {
                List<String> fields = RegistryUtil.parseFIO(fioStr);
                record.setLastName(fields.get(0));
                record.setFirstName(fields.get(1));
                record.setMiddleName(fields.get(2));
            }

            // setup ParseRegistryConstants.date
            String operationDate = messageFieldList.get(++n);
            try {
                record.setOperationDate(ParseRegistryConstants.RECORD_DATE_FORMAT.parseDateTime(operationDate).toDate());
            } catch (Exception e) {
                throw e;
            }

            // validate operation date
            if (registry.getFromDate().after(record.getOperationDate()) ||
                    registry.getTillDate().before(record.getOperationDate())) {

                processLog.error("Failed operation date {} in operation number {} for account {}",
                        new Object[]{record.getOperationDate(), record.getUniqueOperationNumber(),
                                record.getPersonalAccountExt()});
                failed = true;
            }

            // setup unique operation number
            String uniqueOperationNumberStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(uniqueOperationNumberStr)) {
                record.setUniqueOperationNumber(Long.valueOf(uniqueOperationNumberStr));
            }

            // setup amount
            String amountStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(amountStr)) {
                record.setAmount(new BigDecimal(amountStr));
            }

            // setup containers
            String containersStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(containersStr) && !parseContainers(record.getContainers(), containersStr, log)) {
                return null;
            }

            // validate containers
            for (Container container : record.getContainers()) {
                if (!container.getType().isSupport(registry.getType())) {
                    processLog.error("Failed container {} for account {}", container, record.getPersonalAccountExt());
                    failed = true;
                }
            }

            // setup record status
            recordWorkflowManager.setInitialStatus(record, failed);
            if (failed) {
                setErrorStatus(registry);
            }

            return record;
        } catch (Exception e) {
            log.error("Record parse error. Message fields: " + messageFieldList, e);
            setErrorStatus(registry);
        }
        processLog.error("Record number parse error");
        return null;
    }

    public void processFooter(List<String> messageFieldList, Context context, Logger processLog) throws ExecuteException {
        if (messageFieldList.size() < 2) {
            setNextErrorStatus(context);
            processLog.error("Message footer error, invalid number of fields");
            context.addMessageError("footer_invalid_number_fields", context.getRegistry().getRegistryNumber());
        }
    }

    private class Context {
        private Registry registry;

        private List<RegistryRecordData> records = Lists.newArrayList();

        private AtomicInteger recordCounter = new AtomicInteger(0);

        private int numberFlushRegistryRecords;

        private BatchProcessor<JobResult> batchProcessor;

        private IMessenger imessenger;

        private AtomicDouble totalAmount = new AtomicDouble(0);

        private Context(IMessenger imessenger, int numberFlushRegistryRecords) {
            this.numberFlushRegistryRecords = numberFlushRegistryRecords;
            this.imessenger = imessenger;
            batchProcessor = new BatchProcessor<>(10, processor);
        }

        public Registry getRegistry() {
            return registry;
        }

        public void setRegistry(Registry registry) {
            this.registry = registry;
        }

        public List<RegistryRecordData> getRecords() {
            return records;
        }

        public int getRecordCounter() {
            return recordCounter.get();
        }

        private int addRecordCounter(int recordCounter) {
            return this.recordCounter.addAndGet(recordCounter);
        }

        public BigDecimal getTotalAmount() {
            return BigDecimal.valueOf(totalAmount.get());
        }

        public boolean checkTotalAmount() {
            if (registry.getAmount() == null && totalAmount.get() > 0) {
                return false;
            }

            return registry.getAmount() == null || registry.getAmount().equals(getTotalAmount());
        }

        public int getNumberFlushRegistryRecords() {
            return numberFlushRegistryRecords;
        }

        public BatchProcessor<JobResult> getBatchProcessor() {
            return batchProcessor;
        }

        public void add(RegistryRecordData registryRecord) {
            if (registryRecord.getAmount() != null) {
                totalAmount.addAndGet(registryRecord.getAmount().doubleValue());
            }
            records.add(registryRecord);
        }

        public void clearRecords() {
            records = Lists.newArrayList();
        }

        public void addMessageInfo(String message, Object... parameters) {
            imessenger.addMessageInfo(message, parameters);

        }

        public void addMessageError(String message, Object... parameters) {
            imessenger.addMessageError(message, parameters);
        }
    }

    private class RegistryLogger implements InvocationHandler {

        private Logger logger;
        private String incMessage;

        private RegistryLogger(Logger logger, Long registryId) {
            this.logger = logger;
            this.incMessage = registryId > 0? "(Registry : " + registryId + ")" : "";
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
            if (
                    StringUtils.equals(method.getName(), "trace") ||
                    StringUtils.equals(method.getName(), "debug") ||
                    StringUtils.equals(method.getName(), "warn") ||
                    StringUtils.equals(method.getName(), "info") ||
                    StringUtils.equals(method.getName(), "error")
                    ) {

                if (StringUtils.isNotEmpty(incMessage)) {
                    int i = 0;
                    while (i < params.length && !(params[i] instanceof String)) {
                        i++;
                    }
                    if (i < params.length) {
                        String message = ((String)params[i]).concat(incMessage);
                        params[i] = message;
                    }
                }

            }
            return method.invoke(logger, params);
        }
    }
}