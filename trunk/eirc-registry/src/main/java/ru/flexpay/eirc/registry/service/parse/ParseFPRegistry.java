package ru.flexpay.eirc.registry.service.parse;

/**
 * @author Pavel Sknar
 */
public class ParseFPRegistry {
/*
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private RegistryRecordBean registryRecordService;

    private RegistryBean registryService;
    private ServiceBean serviceBean;

    private RegistryFPFileTypeService registryFPFileTypeService;
    @EJB
    private RegistryWorkflowManager registryWorkflowManager;
    @EJB
    private RegistryRecordWorkflowManager recordWorkflowManager;

    private OrganizationService organizationService;
    private ServiceProviderService providerService;

    private CorrectionsService correctionsService;
    private ClassToTypeRegistry typeRegistry;

    private String moduleName;

    private static final Long DEFAULT_NUMBER_READ_CHARS = 32000L;
    private static final Long DEFAULT_NUMBER_FLUSH_REGISTRY_RECORDS = 50L;

    public String parse(File file) throws ExecuteException {
        return parse(file, DEFAULT_NUMBER_READ_CHARS, DEFAULT_NUMBER_FLUSH_REGISTRY_RECORDS);
    }

    public String parse(File file, Long numberReadChars, Long numberFlushRegistryRecords) throws ExecuteException {
        log.debug("start action");

        Logger processLog = log;

        List<RegistryRecord> records = Lists.newArrayList();

        FileReader reader;
        try {
            reader = new FileReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new ExecuteException(e, "Can not open file {}", file.getName());
        }

        try {
            List<FileReader.Message> listMessage = Lists.newArrayList();
            boolean nextIterate;
            do {
                listMessage = getMessages(reader, listMessage, numberReadChars);

                int i = 0;
                for (FileReader.Message message : listMessage) {
                    if (message == null) {
                        finalizeRegistry(parameters, records, numberFlushRegistryRecords, processLog);
                        reader.setInputStream(null);
                        return ParseRegistryConstants.RESULT_END;
                    }
                    i++;
                    String messageValue = message.getBody();
                    if (StringUtils.isEmpty(messageValue)) {
                        continue;
                    }
                    List<String> messageFieldList = StringUtil.splitEscapable(
                            messageValue, ParseRegistryConstants.RECORD_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);

                    Integer messageType = message.getType();

                    if (messageType.equals(FileReader.Message.MESSAGE_TYPE_HEADER)) {
                        Registry registry = processHeader(file, messageFieldList, processLog);
                        if (registry == null) {
                            reader.setInputStream(null);
                            return ProcessInstanceExecuteHandler.RESULT_ERROR;
                        }
                        parameters.put(ParseRegistryConstants.PARAM_REGISTRY_ID, registry.getId());
                        parameters.put(ParseRegistryConstants.PARAM_SERVICE_PROVIDER_ID, ((EircRegistryProperties)registry.getProperties()).getServiceProvider().getId());
                        log.debug("Create registry {}. Add it to process parameters", registry.getId());
                    } else if (messageType.equals(SpFileReader.Message.MESSAGE_TYPE_RECORD)) {
                        RegistryRecord record = processRecord(parameters, messageFieldList, processLog);
                        if (record == null) {
                            reader.setInputStream(null);
                            return ProcessInstanceExecuteHandler.RESULT_ERROR;
                        }
                        records.add(record);
                        if (flushRecordStack(parameters, records, flushNumberRegistryRecords, processLog)) {
                            List<Message> outgoingMessages = listMessage.subList(i, listMessage.size());
                            parameters.put(ParseRegistryConstants.PARAM_MESSAGES, CollectionUtils.list(outgoingMessages));
                            reader.setInputStream(null);
                            parameters.put(ParseRegistryConstants.PARAM_READER, reader);
                            return ProcessInstanceExecuteHandler.RESULT_NEXT;
                        }
                    } else if (messageType.equals(SpFileReader.Message.MESSAGE_TYPE_FOOTER)) {
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
                is.close();
            } catch (IOException e) {
                log.error("Failed reader", e);
                processLog.error("Inner error");
            }
        }
        try {
            reader.setInputStream(null);
        } catch (IOException e) {
            log.error("Inner error", e);
            processLog.error("Inner error");
        }
        return ProcessInstanceExecuteHandler.RESULT_ERROR;
    }

    @SuppressWarnings ({"unchecked"})
    private List<FileReader.Message> getMessages(FileReader reader, List<FileReader.Message> listMessage, Long minReadChars)
            throws ExecuteException, RegistryFormatException {
        if (listMessage == null) {
            listMessage = Lists.newArrayList();
        } else if (!listMessage.isEmpty()) {
            return listMessage;
        }

        try {
            Long startPoint = reader.getPosition();
            FileReader.Message message;

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

    private int flushRecordStack(Map<String, Object> parameters, List<RegistryRecord> records,
                                     Long flushNumberRegistryRecords, Logger processLog) throws ExecuteException {
        return flushRecordStack(records, null, false, flushNumberRegistryRecords, processLog);
    }

    @SuppressWarnings ({"unchecked"})
    private int flushRecordStack(List<RegistryRecord> records, Registry registry, boolean finalize,
                                     Long flushNumberRegistryRecords, Logger processLog) throws ExecuteException {
        if (records != null && (records.size() >= flushNumberRegistryRecords || finalize)) {

            registryRecordService.saveBulk(records);
            int flushedCount = records.size();

            records.clear();
            return flushedCount;
        }
        return -1;
    }

    private void finalizeRegistry(Map<String, Object> parameters, List<RegistryRecord> records,
                                  Long flushNumberRegistryRecords, Logger processLog) throws ExecuteException {
        Registry registry = getRegistry(parameters, processLog);
        if (registry == null) {
            throw new ExecuteException("Registry not found");
        }

        flushRecordStack(parameters, records, registry, true, flushNumberRegistryRecords, processLog);

        log.debug("Finalize registry");

        Long recordCounter = (Long)parameters.get(ParseRegistryConstants.PARAM_NUMBER_PROCESSED_REGISTRY_RECORDS);

        if (!registry.getRecordsNumber().equals(recordCounter)) {
            processLog.error("Registry records number error, expected: {}, found: {}",
                    new Object[]{registry.getRecordsNumber(), recordCounter});
            throw new ExecuteException("Registry records number error, expected: " +
                    registry.getRecordsNumber() + ", found: " + recordCounter);
        }

        try {
            registryWorkflowManager.setNextSuccessStatus(registry);
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
            newRegistry.getFiles().put(registryFPFileTypeService.findByCode(RegistryFPFileType.MB_FORMAT), spFile);
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
                if (!parseContainers(newRegistry, messageFieldList.get(++n), processLog)) {
                    return null;
                }
            }

            processLog.info("Creating new registry: {}", newRegistry);

            newRegistry.setProperties(propertiesFactory.newRegistryProperties());
            EircRegistryProperties props = (EircRegistryProperties) newRegistry.getProperties();
            props.setRegistry(newRegistry);

            Organization recipient = setRecipient(newRegistry, processLog);
            if (recipient == null) {
                processLog.error("Failed processing registry header, recipient not found: #{}", newRegistry.getRecipientCode());
                return null;
            }
            Organization sender = setSender(newRegistry, processLog);
            if (sender == null) {
                processLog.error("Failed processing registry header, sender not found: #{}", newRegistry.getSenderCode());
                return null;
            }
            processLog.info("Recipient: {}\n sender: {}", recipient, sender);

            if (!validateProvider(newRegistry, processLog)) {
                return null;
            }
            ServiceProvider provider = getProvider(newRegistry);
            if (provider == null) {
                processLog.error("Failed processing registry header, provider not found: #{}", newRegistry.getSenderCode());
                return null;
            }
            props.setServiceProvider(provider);

            if (!validateRegistry(newRegistry, processLog)) {
                return null;
            }

            return registryService.create(newRegistry);
        } catch (NumberFormatException e) {
            processLog.error("Header parse error", e);
        } catch (ParseException e) {
            processLog.error("Header parse error", e);
        } catch (TransitionNotAllowed e) {
            processLog.error("Header parse error", e);
        } catch (ExecuteException e) {
            processLog.error("Header parse error", e);
        }
        return null;
    }

    private boolean validateProvider(Registry registry, Logger processLog) {
        if (registry.getType().isPayments()) {
            Stub<Organization> recipient = new Stub<Organization>(registry.getRecipientCode());
            if (recipient.sameId(ApplicationConfig.getSelfOrganization())) {
                processLog.error("Expected service provider recipient, but recieved eirc code");
                return false;
            }
        }
        return true;
    }

    private ServiceProvider getProvider(Registry registry) {
        // for payments registry assume recipient is a service provider
        if (registry.getRegistryType().isPayments()) {
            return providerService.getProvider(new Stub<Organization>(registry.getRecipientCode()));
        }
        return providerService.getProvider(new Stub<Organization>(registry.getSenderCode()));
    }

    private Organization setSender(Registry registry, Logger processLog) {

        EircRegistryProperties props = (EircRegistryProperties) registry.getProperties();

        processLog.debug("Fetching sender via code={}", registry.getSenderCode());
        Organization sender = findOrgByRegistryCorrections(registry, registry.getSenderCode(), processLog);
        if (sender == null) {
            sender = organizationService.readFull(props.getSenderStub());
        }
        props.setSender(sender);
        return sender;
    }

    private Organization setRecipient(Registry registry, Logger processLog) {
        EircRegistryProperties props = (EircRegistryProperties) registry.getProperties();

        Organization recipient;
        if (registry.getRecipientCode() == 0) {
            processLog.debug("Recipient is EIRC, code=0");
            recipient = organizationService.readFull(ApplicationConfig.getSelfOrganizationStub());
        } else {
            processLog.debug("Fetching recipient via code={}", registry.getRecipientCode());
            recipient = findOrgByRegistryCorrections(registry, registry.getRecipientCode(), processLog);
            if (recipient == null) {
                recipient = organizationService.readFull(props.getRecipientStub());
            }
        }
        props.setRecipient(recipient);
        return recipient;
    }

    @Nullable
    private Organization findOrgByRegistryCorrections(Registry registry, Long code, Logger processLog) {

        for (RegistryContainer container : registry.getContainers()) {
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
                    Organization org = organizationService.readFull(stub);
                    if (org == null) {
                        throw new IllegalStateException("Existing master correction for organization " +
                                "references nowhere: " + data);
                    }
                    return org;
                }
            }
        }

        return null;
    }

    private boolean parseContainers(Registry registry, String containersData, Logger processLog) {

        List<String> containers = StringUtil.splitEscapable(
                containersData, ParseRegistryConstants.CONTAINER_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
        for (String data : containers) {
            if (StringUtils.isBlank(data)) {
                continue;
            }
            if (data.length() > org.flexpay.eirc.process.registry.ParseRegistryConstants.MAX_CONTAINER_SIZE) {
                processLog.error("Too long container found: {}", data);
                return false;
            }
            registry.addContainer(new RegistryContainer(data));
        }
        return true;
    }

    private boolean validateRegistry(Registry registry, Logger processLog) {
        List<Registry> persistents = registryService.getRegistries(FilterWrapper.of(registry));
        if (persistents.size() > 0) {
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
        try {
            log.info("adding record: '{}'", StringUtils.join(messageFieldList, '-'));
            int n = 1;
            record.setServiceCode(messageFieldList.get(++n));
            record.setPersonalAccountExt(messageFieldList.get(++n));

            Service service = consumerService.findService(serviceProviderStub, record.getServiceCode());
            if (service == null) {
                processLog.warn("Unknown service code: {}", record.getServiceCode());
            }
            recordProps.setService(service);

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
            DateFormat dateFormat = new SimpleDateFormat(org.flexpay.eirc.process.registry.ParseRegistryConstants.DATE_FORMAT);
            record.setParseConstants.ate(dateFormat.parse(messageFieldList.get(++n)));

            // setup unique ParseRegistryConstants.number
            String uniqueParseConstants.umberStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(uniqueParseConstants.umberStr)) {
                record.setUniqueParseConstants.umber(Long.valueOf(uniqueParseConstants.umberStr));
            }

            // setup amount
            String amountStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(amountStr)) {
                record.setAmount(new BigDecimal(amountStr));
            }

            // setup containers
            String containersStr = messageFieldList.get(++n);
            if (StringUtils.isNotEmpty(containersStr)) {
                record.setContainers(parseContainers(record, containersStr));
            }

            // setup record status
            recordWorkflowManager.setInitialStatus(record);

            return record;
        } catch (NumberFormatException e) {
            log.error("Record number parse error", e);
        } catch (ParseException e) {
            log.error("Record parse error", e);
        } catch (RegistryFormatException e) {
            log.error("Record number parse error", e);
        } catch (TransitionNotAllowed transitionNotAllowed) {
            log.error("Record number parse error", transitionNotAllowed);
        } catch (ExecuteException e) {
            log.error("Record number parse error", e);
        }
        processLog.error("Record number parse error");
        return null;
    }

    private List<RegistryRecordContainer> parseContainers(RegistryRecord record, String containersData)
            throws RegistryFormatException {

        List<String> containers = StringUtil.splitEscapable(
                containersData, ParseRegistryConstants.CONTAINER_DELIMITER, ParseRegistryConstants.ESCAPE_SYMBOL);
        List<RegistryRecordContainer> result = new ArrayList<RegistryRecordContainer>(containers.size());
        int n = 0;
        for (String data : containers) {
            if (StringUtils.isBlank(data)) {
                continue;
            }
            if (data.length() > org.flexpay.eirc.process.registry.ParseRegistryConstants.MAX_CONTAINER_SIZE) {
                throw new RegistryFormatException("Too long container found: " + data);
            }
            RegistryRecordContainer container = new RegistryRecordContainer();
            container.setOrder(n++);
            container.setRecord(record);
            container.setData(data);
            result.add(container);
        }

        return result;
    }

    public void processFooter(List<String> messageFieldList, Logger processLog) throws ExecuteException {
        if (messageFieldList.size() < 2) {
            processLog.error("Message footer error, invalid number of fields");
            throw new ExecuteException("Message footer error, invalid number of fields");
        }
    }
    */
}
