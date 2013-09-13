package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.ConfigBean;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.*;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;

import static ru.flexpay.eirc.registry.service.parse.FileReader.Message;

/**
 * @author Pavel Sknar
 */
@Stateless
public class RegistryParser implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(RegistryParser.class);

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

    public Registry parse(File file) throws ExecuteException {
        int numberReadChars = configBean.getInteger(RegistryConfig.NUMBER_READ_CHARS, true);
        int numberFlushRegistryRecords = configBean.getInteger(RegistryConfig.NUMBER_FLUSH_REGISTRY_RECORDS, true);
        return parse(file, numberReadChars, numberFlushRegistryRecords);
    }

    public Registry parse(File file, int numberReadChars, int numberFlushRegistryRecords) throws ExecuteException {
        log.debug("start action");

        Logger processLog = log;

        Context context = new Context(numberFlushRegistryRecords);

        FileReader reader;
        try {
            reader = new FileReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new ExecuteException(e, "Can not open file {}", file.getName());
        }

        try {
            List<Message> listMessage = Lists.newArrayList();
            boolean nextIterate;
            Registry registry = null;
            do {
                listMessage = getMessages(reader, listMessage, numberReadChars);

                int i = 0;
                for (Message message : listMessage) {
                    if (message == null) {
                        finalizeRegistry(context, processLog);
                        return registry;
                    }
                    i++;
                    String messageValue = message.getBody();
                    if (StringUtils.isEmpty(messageValue)) {
                        continue;
                    }
                    List<String> messageFieldList = StringUtil.splitEscapable(
                            messageValue, ParseRegistryConstants.RECORD_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);

                    Integer messageType = message.getType();

                    if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_HEADER)) {
                        registry = processHeader(file, messageFieldList, processLog);
                        if (registry == null) {
                            return null;
                        }
                        context.setRegistry(registry);
                        log.debug("Create registry {}. Add it to process parameters", registry.getId());
                    } else if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_RECORD)) {
                        RegistryRecord record = processRecord(registry, messageFieldList, processLog);
                        if (record == null) {
                            reader.setInputStream(null);
                            return null;
                        }
                        context.add(record);
                        flushRecordStack(context, processLog);
                    } else if (messageType.equals(ParseRegistryConstants.MESSAGE_TYPE_FOOTER)) {
                        processFooter(messageFieldList, processLog);
                    }
                }
                nextIterate = !listMessage.isEmpty();
                listMessage.clear();

            } while(nextIterate);
            log.error("Failed registry file");
        } catch(Exception e) {
            log.error("Processing error", e);
            processLog.error("Inner error");
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Failed reader", e);
                processLog.error("Inner error");
            }
        }

        return null;
    }

    @SuppressWarnings ({"unchecked"})
    private List<Message> getMessages(FileReader reader, List<Message> listMessage, Integer minReadChars)
            throws ExecuteException, RegistryFormatException {
        if (listMessage == null) {
            listMessage = Lists.newArrayList();
        } else if (!listMessage.isEmpty()) {
            return listMessage;
        }

        try {
            Long startPoint = reader.getPosition();
            Message message;

            do {
                message = reader.readMessage();
                listMessage.add(message);
            } while (message != null && (reader.getPosition() - startPoint) < minReadChars);
            log.debug("read {} number record", listMessage.size());

            return listMessage;
        } catch (IOException e) {
            throw new ExecuteException("Failed open stream", e);
        }
    }

    private void flushRecordStack(Context context, Logger processLog) throws ExecuteException {
        flushRecordStack(context, false, processLog);
    }

    @SuppressWarnings ({"unchecked"})
    private void flushRecordStack(final Context context, boolean finalize, Logger processLog) throws ExecuteException {
        if (context.getRecords() != null &&
                (context.getRecords().size() >= context.getNumberFlushRegistryRecords() || finalize)) {

            final List<RegistryRecord> records = context.getRecords();

            context.getBatchProcessor().processJob(new AbstractJob<JobResult>() {
                @Override
                public JobResult execute() throws ExecuteException {
                    registryRecordService.saveBulk(records);
                    //TODO save intermediate state
                    context.setRecordCounter(context.getRecordCounter() + records.size());
                    return JobResult.SUCCESSFUL;
                }
            });

            context.clearRecords();
        }
    }

    private void finalizeRegistry(Context context, Logger processLog) throws ExecuteException {

        flushRecordStack(context, true, processLog);

        context.getBatchProcessor().waitEndWorks();

        log.debug("Finalize registry");

        if (context.getRegistry().getRecordsCount() != context.getRecordCounter()) {
            processLog.error("Registry records number error, expected: {}, found: {}",
                    new Object[]{context.getRegistry().getRecordsCount(), context.getRecordCounter()});
            throw new ExecuteException("Registry records number error, expected: " +
                    context.getRegistry().getRecordsCount() + ", found: " + context.getRecordCounter());
        }

        try {
            registryWorkflowManager.setNextSuccessStatus(context.getRegistry());
        } catch (TransitionNotAllowed transitionNotAllowed) {
            throw new ExecuteException("Does not finalize registry", transitionNotAllowed);
        }
    }

    private Registry processHeader(File file, List<String> messageFieldList, Logger processLog) {
        if (messageFieldList.size() < 10) {
            processLog.error("Message header error, invalid number of fields: {}, expected at least 10", messageFieldList.size());
            return null;
        }

        processLog.info("Adding header: {}", messageFieldList);

        DateFormat dateFormat = ParseRegistryConstants.DATE_FORMAT;

        Registry newRegistry = new Registry();
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
                processLog.error("Unknown registry type field: {}", value);
                return null;
            }
            newRegistry.setType(registryType);
            newRegistry.setRecordsCount(Integer.valueOf(messageFieldList.get(++n)));
            newRegistry.setCreationDate(dateFormat.parse(messageFieldList.get(++n)));
            newRegistry.setFromDate(dateFormat.parse(messageFieldList.get(++n)));
            newRegistry.setTillDate(dateFormat.parse(messageFieldList.get(++n)));
            newRegistry.setSenderOrganizationId(Long.valueOf(messageFieldList.get(++n)));
            newRegistry.setRecipientOrganizationId(Long.valueOf(messageFieldList.get(++n)));
            String amountStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(amountStr)) {
                newRegistry.setAmount(new BigDecimal(amountStr));
            }
            if (messageFieldList.size() > n) {
                if (!parseContainers(newRegistry.getContainers(), messageFieldList.get(++n), processLog)) {
                    return null;
                }
            }

            processLog.info("Creating new registry: {}", newRegistry);

            Organization recipient = getRecipient(newRegistry, processLog);
            if (recipient == null) {
                processLog.error("Failed processing registry header, recipient not found: #{}", newRegistry.getRecipientOrganizationId());
                return null;
            }
            Organization sender = getSender(newRegistry, processLog);
            if (sender == null) {
                processLog.error("Failed processing registry header, sender not found: #{}", newRegistry.getSenderOrganizationId());
                return null;
            }
            processLog.info("Recipient: {}\n sender: {}", recipient, sender);

            if (!validateServiceProvider(newRegistry, processLog)) {
                return null;
            }

            if (!validateRegistry(newRegistry, processLog)) {
                return null;
            }

            newRegistry.setLoadDate(DateUtil.getCurrentDate());

            registryService.save(newRegistry);

            return newRegistry;
        } catch (NumberFormatException | ParseException | TransitionNotAllowed e) {
            processLog.error("Header parse error", e);
        }
        return null;
    }

    private boolean validateServiceProvider(Registry registry, Logger processLog) {
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

    private Organization getRecipient(Registry registry, Logger processLog) {

        Organization recipient;
        if (registry.getRecipientOrganizationId() == 0) {
            processLog.debug("Recipient is EIRC, code=0");
            configBean.getConfigs();
            recipient = organizationStrategy.findById(configBean.getInteger(RegistryConfig.SELF_ORGANIZATION_ID, true), false);
        } else {
            processLog.debug("Fetching recipient via code={}", registry.getRecipientOrganizationId());
            recipient = findOrgByRegistryCorrections(registry, registry.getSenderOrganizationId(), processLog);
            if (recipient == null) {
                recipient = organizationStrategy.findById(registry.getRecipientOrganizationId(), false);
            }
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
            if (data.length() < 1) {
                processLog.error("Failed container format: {}", containerData);
                return false;
            }

            ContainerType containerType = ContainerType.valueOf(Long.getLong(containerData.get(0), 0L));

            distContainers.add(new Container(data, containerType));
        }
        return true;
    }

    private boolean validateRegistry(Registry registry, Logger processLog) {
        int countRegistries = registryService.count(FilterWrapper.of(registry));
        if (countRegistries > 0) {
            processLog.error("Registry was already uploaded");
            return false;
        }
        return true;
    }

    private RegistryRecord processRecord(Registry registry, List<String> messageFieldList, Logger processLog) {
        if (messageFieldList.size() < 10) {
            processLog.error("Message record error, invalid number of fields: {}", messageFieldList.size());
            return null;
        }

        RegistryRecord record = new RegistryRecord();
        record.setRegistryId(registry.getId());
        try {
            log.info("adding record: '{}'", StringUtils.join(messageFieldList, '-'));
            int n = 1;
            record.setServiceCode(messageFieldList.get(++n));
            record.setPersonalAccountExt(messageFieldList.get(++n));

            //TODO find by external id, if service code started by # (maybe using correction)
            Service service = serviceBean.getService(Long.getLong(record.getServiceCode(), 0L));
            if (service == null) {
                processLog.warn("Unknown service code: {}", record.getServiceCode());
            }

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
                record.setTownName(addressFieldList.get(0));
                record.setStreetType(addressFieldList.get(1));
                record.setStreetName(addressFieldList.get(2));
                record.setBuildingNum(addressFieldList.get(3));
                record.setBuildingBulkNum(addressFieldList.get(4));
                record.setApartmentNum(addressFieldList.get(5));
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
            record.setOperationDate(ParseRegistryConstants.DATE_FORMAT.parse(messageFieldList.get(++n)));

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

            // setup record status
            recordWorkflowManager.setInitialStatus(record);

            return record;
        } catch (NumberFormatException | RegistryFormatException | TransitionNotAllowed e) {
            log.error("Record number parse error", e);
        } catch (ParseException e) {
            log.error("Record parse error", e);
        }
        processLog.error("Record number parse error");
        return null;
    }

    public void processFooter(List<String> messageFieldList, Logger processLog) throws ExecuteException {
        if (messageFieldList.size() < 2) {
            processLog.error("Message footer error, invalid number of fields");
            throw new ExecuteException("Message footer error, invalid number of fields");
        }
    }

    private class Context {
        private Registry registry;

        private List<RegistryRecord> records = Lists.newArrayList();

        private volatile int recordCounter = 0;

        private int numberFlushRegistryRecords;

        private BatchProcessor<JobResult> batchProcessor;

        private Context(int numberFlushRegistryRecords) {
            this.numberFlushRegistryRecords = numberFlushRegistryRecords;
            batchProcessor = new BatchProcessor<>(10, processor);
        }

        private Registry getRegistry() {
            return registry;
        }

        private void setRegistry(Registry registry) {
            this.registry = registry;
        }

        private List<RegistryRecord> getRecords() {
            return records;
        }

        private int getRecordCounter() {
            return recordCounter;
        }

        private void setRecordCounter(int recordCounter) {
            this.recordCounter = recordCounter;
        }

        private int getNumberFlushRegistryRecords() {
            return numberFlushRegistryRecords;
        }

        private BatchProcessor<JobResult> getBatchProcessor() {
            return batchProcessor;
        }

        public void add(RegistryRecord registryRecord) {
            records.add(registryRecord);
        }

        public void clearRecords() {
            records = Lists.newArrayList();
        }
    }
}
