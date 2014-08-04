package ru.flexpay.eirc.mb_transformer.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.complitex.correction.entity.OrganizationCorrection;
import org.complitex.correction.service.OrganizationCorrectionBean;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.complitex.dictionary.util.DateUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;
import ru.flexpay.eirc.mb_transformer.util.MbParsingConstants;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.AbstractJob;
import ru.flexpay.eirc.registry.service.AbstractMessenger;
import ru.flexpay.eirc.registry.service.handle.MbConverterQueueProcessor;
import ru.flexpay.eirc.registry.service.parse.FileReader;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;
import ru.flexpay.eirc.registry.service.parse.RegistryFormatException;
import ru.flexpay.eirc.registry.util.FileUtil;
import ru.flexpay.eirc.registry.util.ParseUtil;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.correction.entity.ServiceCorrection;
import ru.flexpay.eirc.service.correction.service.ServiceCorrectionBean;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants.*;


/**
 * Generate the payments registry in MB format.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EircPaymentsRegistryConverter {

	private static final Logger log = LoggerFactory.getLogger(EircPaymentsRegistryConverter.class.getClass());

    private static final int NUMBER_READ_CHARS = 10000;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy");
	private static final DateTimeFormatter EIRC_PAYMENT_DATE_FORMATTER = DateTimeFormat.forPattern("ddMMyyyy");
	private static final DateTimeFormatter MB_OPERATION_DATE_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
	private static final DateTimeFormatter PAYMENT_PERIOD_DATE_FORMATTER = DateTimeFormat.forPattern("yyyyMM");

    private static final String FORMAT_FILE_NAME = "00001001.YMD";

	private static final String[] TABLE_HEADERS = {
			"код квит",
			"л.с. ЕРЦ ",
			"  л.с.    ",
			" Ф. И. О.      ",
			"   ",
			" Улица          ",
			"Дом    ",
			"Кв. ",
			"Услуга       ",
			" Нач. ",
			" Кон. ",
			"Рзн",
			"Дата пл.",
			"   с  ",
			"   по ",
			"Всего  "
	};
	private static final Map<String, String> SERVICE_NAMES = ImmutableMap.<String, String>builder()
            .put("01", "ЭЛЕКТР  ")
            .put("02", "КВ/ЭКСПЛ") // точно известно
            .put("03", "ОТОПЛ   ")
            .put("04", "ГОР ВОДА")
            .put("05", "ХОЛ ВОДА")
            .put("06", "КАНАЛИЗ ")
            .put("07", "ГАЗ ВАР ")
            .put("08", "ГАЗ ОТОП")
            .put("09", "РАДИО   ")
            .put("10", "АНТ     ") // точно известно
            .put("11", "ЖИВ     ") // точно известно
            .put("12", "ГАРАЖ   ") // точно известно
            .put("13", "ПОГРЕБ  ") // точно известно
            .put("14", "САРАЙ   ") // точно известно
            .put("15", "КЛАДОВКА") // точно известно
            .put("16", "ТЕЛЕФОН ")
            .put("19", "АССЕНИЗ ")
            .put("20", "ЛИФТ    ")
            .put("21", "ХОЗ РАСХ") // точно известно
            .put("22", "НАЛ ЗЕМЛ")
            .put("23", "ПОВ ПОДК")
            .put("24", "ОПЛ АКТ ")
            .put("25", "РЕМ СЧЁТ")
            .build();

    private final static byte[] DELIMITER = "|".getBytes(MbParsingConstants.REGISTRY_FILE_CHARSET);

    @EJB
    private EircOrganizationStrategy eircOrganizationStrategy;

    @EJB
    private OrganizationCorrectionBean organizationCorrectionBean;

    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private MbConverterQueueProcessor mbConverterQueueProcessor;

    @EJB(name = "MbTransformerConfigBean")
    private MbTransformerConfigBean configBean;

    private Cache<String, String> serviceCorrectionCache = CacheBuilder.newBuilder().
            maximumSize(1000).
            expireAfterWrite(10, TimeUnit.MINUTES).
            build();

    private Cache<String, Service> serviceCache = CacheBuilder.newBuilder().
            maximumSize(1000).
            expireAfterWrite(10, TimeUnit.MINUTES).
            build();


    public void exportToMegaBank(final FileReader reader, final String dir, final String mbFileName,
                                 final Long mbOrganizationId, final Long eircOrganizationId,
                                 final AbstractMessenger imessenger, final AbstractFinishCallback finishConvert) throws AbstractException {
        imessenger.addMessageInfo("eirc_payments_convert_starting", reader.getFileName());
        finishConvert.init();

        mbConverterQueueProcessor.execute(
                new AbstractJob<Void>() {
                    @Override
                    public Void execute() throws ExecuteException {
                        try {
                            exportToMegaBank(reader, dir, mbFileName, mbOrganizationId, eircOrganizationId, imessenger);
                        } catch (Exception e) {
                            log.error("Can not convert files", e);
                            imessenger.addMessageError("eirc_payments_fail_convert", e.toString());
                        } finally {
                            imessenger.addMessageInfo("eirc_payments_convert_finish", reader.getFileName());
                            finishConvert.complete();
                        }
                        return null;
                    }
                }
        );
    }

    public void exportToMegaBank(final FileReader reader, String dir, String mbFileName,
                                 Long mbOrganizationId, Long eircOrganizationId, final AbstractMessenger imessenger) throws IOException,
            RegistryFormatException, ExecuteException {
        final AbstractMessenger fileMessenger = new FileProxyMessenger(imessenger, reader.getFileName());
        try {
            DataSource dataSource = new DataSource() {
                List<FileReader.Message> listMessage = Lists.newArrayList();
                int idx = 0;
                int totalCount = 0;

                Registry registry = null;


                @Override
                public Registry getRegistry() {
                    if (registry != null) {
                        return registry;
                    }
                    FileReader.Message message = null;
                    try {
                        message = getNextMessage();
                    } catch (RegistryFormatException | ExecuteException e ) {
                        fileMessenger.addMessageError("eirc_payments_fail_convert", e.toString());
                        log.error("Failed message", e);
                    }
                    if (message == null) {
                        return null;
                    }
                    if (message.getType() != MESSAGE_TYPE_HEADER) {
                        fileMessenger.addMessageError("eirc_registry_wanted_header");
                        log.error("Failed registry format: wanted header");
                        return null;
                    }
                    registry = new Registry();
                    List<String> listMessage = parseMessage(message.getBody());
                    try {
                        ParseUtil.fillUpRegistry(listMessage, ParseRegistryConstants.HEADER_DATE_FORMAT, registry);
                        return registry;
                    } catch (RegistryFormatException e) {
                        fileMessenger.addMessageError("eirc_payments_fail_convert", e.toString());
                        log.error("Failed fill up registry", e.toString());
                    }
                    return null;
                }

                @Override
                public RegistryRecordData getNextRecord() throws AbstractException, IOException {
                    if (registry == null && getRegistry() == null) {
                        return null;
                    }
                    FileReader.Message message = getNextMessage();
                    if (message == null) {
                        return null;
                    }
                    if (message.getType() == MESSAGE_TYPE_FOOTER) {
                        fileMessenger.addMessageInfo("eirc_registry_success_header", totalCount);
                        log.info("Find footer. End of file");
                        return null;
                    }
                    if (message.getType() != MESSAGE_TYPE_RECORD) {
                        fileMessenger.addMessageError("eirc_registry_wanted_record");
                        log.error("Failed registry format: wanted record");
                        return null;
                    }
                    List<String> listMessage = parseMessage(message.getBody());
                    RegistryRecord record = new RegistryRecord();
                    return ParseUtil.fillUpRecord(listMessage, record)? record : null;
                }

                private FileReader.Message getNextMessage() throws RegistryFormatException, ExecuteException {
                    if (idx >= listMessage.size()) {
                        idx = 0;
                        listMessage = reader.getMessages(listMessage, NUMBER_READ_CHARS);
                        if (listMessage.isEmpty()) {
                            return null;
                        }
                    }
                    totalCount++;
                    FileReader.Message message = listMessage.get(idx++);
                    if (message != null && totalCount%10000 == 0) {
                        fileMessenger.addMessageInfo("processed_lines", totalCount);
                    }
                    return message;
                }

                private List<String> parseMessage(String message) {
                    return StringUtil.splitEscapable(
                            message, RECORD_DELIMITER, ESCAPE_SYMBOL);
                }
            };

            Registry registry = dataSource.getRegistry();
            if (registry == null) {
                return;
            }

            Organization serviceProvider = eircOrganizationStrategy.findById(getDataSource(), registry.getRecipientOrganizationId(), true);
            if (serviceProvider == null) {
                fileMessenger.addMessageError("eirc_payments_recipient_not_found", registry.getRecipientOrganizationId());
                log.error("Service provider {} not found", registry.getRecipientOrganizationId());
                return;
            }

            Organization sender = eircOrganizationStrategy.findById(getDataSource(), registry.getSenderOrganizationId(), true);
            if (sender == null) {
                fileMessenger.addMessageError("eirc_payments_sender_not_found", registry.getSenderOrganizationId());
                log.error("Sender {} not found", registry.getSenderOrganizationId());
                return;
            }

            try {
                exportToMegaBank(dataSource, serviceProvider, mbOrganizationId, eircOrganizationId, dir,
                        mbFileName, 2*reader.getFileLength() + 2048, fileMessenger);
            } catch (Exception e) {
                fileMessenger.addMessageError("eirc_payments_fail_convert", e.toString());
                log.error("Failed export EIRC payments to MB payments", e);
            }


        } finally {
            reader.close();
        }
    }

	/**
	 * Export EIRC registry to MB registry file.
	 *
	 * @param serviceProviderOrganization Service provider organization
	 * @throws ExecutionException
	 */
	public void exportToMegaBank(DataSource dataSource, Organization serviceProviderOrganization,
                                     Long mbOrganizationId, Long eircOrganizationId,
                                     String dir, String mbFileName, long mbFileLength,
                                     AbstractMessenger imessenger)
            throws ExecutionException, AbstractException {

        Registry registry = dataSource.getRegistry();

        FileChannel outChannel = null;

        File outFile = null;
        
		try {

            String eircDataSource = getDataSource();

            Container detailsPaymentsDocument = registry.getContainer(ContainerType.DETAILS_PAYMENTS_DOCUMENT);
            if (detailsPaymentsDocument == null) {
                imessenger.addMessageError("eirc_payments_not_found_details");
                log.error("Did not find details of payments");
                return;
            }
            String[] detailsData = detailsPaymentsDocument.getData().split(":");

            Date paymentDate = EIRC_PAYMENT_DATE_FORMATTER.parseDateTime(detailsData[2]).toDate();
            Date creationDate = registry.getCreationDate();

            if (StringUtils.isEmpty(mbFileName)) {

                String externalServiceProviderId = getExternalServiceProviderId(registry, serviceProviderOrganization,
                        mbOrganizationId, eircOrganizationId, eircDataSource);

                if (externalServiceProviderId.length() > 5) {
                    throw new IllegalArgumentException("Service provider code '" + externalServiceProviderId + "' have length more 5");
                }

                String year = String.valueOf(DateUtil.getYear(creationDate));

                StringBuilder builder = new StringBuilder(FORMAT_FILE_NAME);
                builder.replace(5 - externalServiceProviderId.length(), 5, externalServiceProviderId);
                builder.setCharAt(9, year.charAt(year.length() - 1));
                builder.setCharAt(10, mod31(DateUtil.getMonth(creationDate) + 1));
                builder.setCharAt(11, mod31(DateUtil.getDay(creationDate)));

                mbFileName = builder.toString();
            }

            outFile = new File(dir, mbFileName);

            outChannel = new RandomAccessFile(outFile, "rw").getChannel();
            ByteBuffer buffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, mbFileLength);

            log.info("Writing service lines");

            writeCharToLine(buffer, '_', 128);
            buffer.put(new byte[306]);
            writeLine(buffer, "");
            writeCharToLine(buffer, '_', 128);

			// заголовочные строки
			writeLine(buffer, "\tРеестр поступивших платежей. Мемориальный ордер №" + detailsData[1]);
			writeLine(buffer, "\tДля \"" + eircOrganizationStrategy.displayDomainObject(serviceProviderOrganization, getLocation()) + "\". День распределения платежей " +
						 DATE_FORMATTER.print(paymentDate.getTime()) + ".");
			writeCharToLine(buffer, ' ', 128);
			writeCharToLine(buffer, ' ', 128);
			BigDecimal amount = registry.getAmount();
			if (amount == null) {
				amount = new BigDecimal(0);
			}

			writeLine(buffer, "\tВсего " + (amount.multiply(new BigDecimal("100")).intValue()) +
						 " коп. Суммы указаны в копейках. Всего строк " + registry.getRecordsCount());
			writeCharToLine(buffer, ' ', 128);

			// шапка таблицы
			writeLine(buffer, TABLE_HEADERS, "|");
			StringBuilder builder = new StringBuilder();
			for (String s : TABLE_HEADERS) {
				builder.append('+');
				for (int i = 0; i < s.length(); i++) {
					builder.append('-');
				}
			}
			writeLine(buffer, builder.toString());

			// информационные строки
            log.debug("Write info lines");
			log.debug("Total info lines: {}", registry.getRecordsCount());

            RegistryRecordData registryRecord;
            while ((registryRecord = dataSource.getNextRecord()) != null){
                writeInfoLine(buffer, registryRecord, serviceProviderOrganization.getId(), eircOrganizationId, mbOrganizationId, eircDataSource);
			}

            outChannel.truncate(buffer.position());

            buffer.clear();

            imessenger.addMessageInfo("mb_payments_created", mbFileName, registry.getRecordsCount());

		} catch (Exception e) {
            if (outFile != null && outFile.exists()) {
                outFile.delete();
            }
			throw new ExecutionException(e);
		} finally {
            IOUtils.closeQuietly(outChannel);
		}
	}

	private String getExternalServiceProviderId(Registry registry, Organization serviceProviderOrganization,
                                                Long mbOrganizationId, Long eircOrganizationId, String dataSource) throws MbConverterException {

        List<OrganizationCorrection> organizationCorrections = organizationCorrectionBean.getOrganizationCorrections(dataSource,
            FilterWrapper.of(new OrganizationCorrection(null, serviceProviderOrganization.getId(), null,
                    registry.getSenderOrganizationId(), eircOrganizationId, null)));
        if (organizationCorrections.size() <= 0) {
            throw new MbConverterException("No service provider correction with id {0}: organizationId={1}, userOrganizationId={2}",
                    serviceProviderOrganization.getId(), registry.getSenderOrganizationId(), eircOrganizationId);
        }
        if (organizationCorrections.size() > 1) {
            throw new MbConverterException("Found several correction for service provider {0}: organizationId={1}, userOrganizationId={2}",
                    serviceProviderOrganization.getId(), registry.getSenderOrganizationId(), eircOrganizationId);
        }
        return organizationCorrections.get(0).getCorrection();
	}

    public String getOutServiceCode(String innerServiceCode, Long mbOrganizationId, Long eircOrganizationId, String dataSource) throws MbConverterException {
        String serviceCode = serviceCorrectionCache.getIfPresent(innerServiceCode);
        if (serviceCode != null) {
            return serviceCode;
        }

        Service service = getService(innerServiceCode, dataSource);
        List<ServiceCorrection> serviceCorrections = serviceCorrectionBean.getServiceCorrections(dataSource,
                FilterWrapper.of(new ServiceCorrection(null, service.getId(), null, mbOrganizationId,
                        eircOrganizationId, null))
        );
        if (serviceCorrections.size() <= 0) {
            throw new MbConverterException(
                    "No found external correction for service code {0}", innerServiceCode);
        }
        if (serviceCorrections.size() > 1) {
            throw new MbConverterException("Found several correction for service with code {0}", innerServiceCode);
        }
        serviceCode = String.valueOf(serviceCorrections.get(0).getObjectId());
        serviceCorrectionCache.put(innerServiceCode, serviceCode);

        return serviceCode;
    }

    public Service getService(String innerServiceCode, String dataSource) throws MbConverterException {
        Service service = serviceCache.getIfPresent(innerServiceCode);
        if (service != null) {
            return service;
        }
        List<Service> services = serviceBean.getServices(dataSource, FilterWrapper.of(new Service(innerServiceCode)));
        if (services.size() == 0) {
            throw new MbConverterException(
                    "No found service with code {0}", innerServiceCode);
        }
        if (services.size() > 1) {
            throw new MbConverterException("Found several services with code {0}", innerServiceCode);
        }
        service = services.get(0);
        serviceCache.put(innerServiceCode, service);
        return service;
    }

    private void writeInfoLine(ByteBuffer buffer, RegistryRecordData record,
                               Long serviceProviderId, Long eircOrganizationId, Long mbOrganizationId,
                               String dataSource)
            throws ExecutionException, MbConverterException, IOException {

		//граница таблицы
		//writeCellData(buffer, DELIMITER, "", 0, ' ');

		// номер квитанции
        String numberQuittance = null;
        for (Container container : record.getContainers()) {
            if (container.getType().equals(ContainerType.CASH_PAYMENT) ||
                    container.getType().equals(ContainerType.CASHLESS_PAYMENT)) {
                String[] data = container.getData().split(":");
                numberQuittance = data[2];
            }
        }
		writeCellData(buffer, DELIMITER, numberQuittance, TABLE_HEADERS[0].length(), ' ');

		// лиц. счёт ЕРЦ
		String eircAccount  = getEircAccount(record, serviceProviderId, eircOrganizationId, mbOrganizationId);
		writeCellData(buffer, DELIMITER, eircAccount, TABLE_HEADERS[1].length(), ' ');

		// лиц. счёт поставщика услуг
		writeCellData(buffer, DELIMITER, record.getPersonalAccountExt(), TABLE_HEADERS[2].length(), ' ');

		// ФИО
		String fio = record.getLastName();
		if (record.getFirstName() != null && record.getFirstName().length() > 0) {
			fio += " " + record.getFirstName().charAt(0);
			if (record.getMiddleName() != null && record.getMiddleName().length() > 0) {
				fio += " " + record.getMiddleName().charAt(0);
			}
		}
		writeCellData(buffer, DELIMITER, fio, TABLE_HEADERS[3].length(), ' ');

		// тип улицы
		String streetType = record.getStreetType();
		if (streetType != null && streetType.length() > 3) {
			streetType = streetType.substring(0, 2);
		}
		writeCellData(buffer, DELIMITER, streetType, TABLE_HEADERS[4].length(), ' ');

		// название улицы
		writeCellData(buffer, DELIMITER, record.getStreet(), TABLE_HEADERS[5].length(), ' ');

		// дом
		String building = record.getBuildingNumber();
		if (building != null && record.getBuildingCorp() != null) {
			building += " " + record.getBuildingCorp();
		}
		writeCellData(buffer, DELIMITER, building, TABLE_HEADERS[6].length(), ' ');

		// квартира
		writeCellData(buffer, DELIMITER, record.getApartment(), TABLE_HEADERS[7].length(), ' ');

		// услуга
		String serviceCode = record.getServiceCode();
		if (serviceCode == null) {
			throw new MbConverterException("Registry record`s service code is null. Registry record Id: " + record.getId());
		}
		serviceCode = getOutServiceCode(serviceCode, mbOrganizationId, eircOrganizationId, dataSource);
		if (serviceCode == null) {
			return;
		}
		serviceCode = StringUtils.leftPad(serviceCode, 2, '0');

		String service = serviceCode + "." + SERVICE_NAMES.get(serviceCode) + " " + "*";
		writeCellData(buffer, DELIMITER, service, TABLE_HEADERS[8].length(), ' ');

		// начальное показание счётчика
		writeCellData(buffer, DELIMITER, "0", TABLE_HEADERS[9].length(), ' ');

		// конечное показание счётчика
		writeCellData(buffer, DELIMITER, "0", TABLE_HEADERS[10].length(), ' ');

		// разница показаний счётчика
		writeCellData(buffer, DELIMITER, "0", TABLE_HEADERS[11].length(), ' ');

		// дата платежа
		Date operationDate = record.getOperationDate();
		String paymentDate = operationDate != null ? MB_OPERATION_DATE_FORMATTER.print(operationDate.getTime()) : null;
		writeCellData(buffer, DELIMITER, paymentDate, TABLE_HEADERS[12].length(), ' ');

		// с какого месяца оплачена услуга
		String paymentMonth = null;
		if (operationDate != null) {
			Calendar cal = (Calendar) Calendar.getInstance().clone();
			cal.setTime(operationDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.roll(Calendar.MONTH, -1);
			paymentMonth = PAYMENT_PERIOD_DATE_FORMATTER.print(cal.getTime().getTime());
		}
		writeCellData(buffer, DELIMITER, paymentMonth, TABLE_HEADERS[13].length(), ' ');

		// по какой месяц оплачена услуга
		writeCellData(buffer, DELIMITER, paymentMonth, TABLE_HEADERS[14].length(), ' ');

		// сумма (значение суммы изначально передаётся в рублях, но должно быть записано в копейках)\
		int sum = record.getAmount().multiply(new BigDecimal("100")).intValue();
		writeCellData(buffer, DELIMITER, String.valueOf(sum), null, ' ');

        writeLine(buffer, null);

	}

    @SuppressWarnings("unused")
    private String getEircAccount(RegistryRecordData record, Long serviceProviderId, Long eircOrganizationId, Long mbOrganizationId) throws MbConverterException {
        return "";
        /*
        List<ServiceProviderAccount> serviceProviderAccounts = serviceProviderAccountBean.getServiceProviderAccounts(
                FilterWrapper.of(new ServiceProviderAccount(record.getPersonalAccountExt(), serviceProviderId, getService(record.getServiceCode()))));
        if (serviceProviderAccounts.size() == 0) {
            throw new MbConverterException("No found service provider account by {0} {1} {2}",
                    record.getPersonalAccountExt(), serviceProviderId, record.getServiceCode());
        }
        if (serviceProviderAccounts.size() > 1) {
            throw new MbConverterException("Found several service provider accounts with {0} {1} {2}",
                    record.getPersonalAccountExt(), serviceProviderId, record.getServiceCode());
        }
        List<ServiceProviderAccountCorrection> serviceProviderAccountCorrections = serviceProviderAccountCorrection.
                getServiceProviderAccountCorrections(FilterWrapper.of(
                        new ServiceProviderAccountCorrection(null, serviceProviderAccounts.get(0).getId(), null, mbOrganizationId, eircOrganizationId, null)));
        if (serviceProviderAccountCorrections.size() <= 0) {
            throw new MbConverterException(
                    "No found correction for service provider account {0}", serviceProviderAccounts.get(0));
        }
        if (serviceProviderAccountCorrections.size() > 1) {
            throw new MbConverterException("Found several correction for service provider account {0}", serviceProviderAccounts.get(0));
        }
        return serviceProviderAccountCorrections.get(0).getCorrection();
        */
    }

    private void writeCellData(ByteBuffer buffer, byte[] delimiter, String data, Integer length, char ch) {
        String cell = createCellData(data, length, ch);
        buffer.put(delimiter);
        buffer.put(cell.getBytes(MbParsingConstants.REGISTRY_FILE_CHARSET));
    }

	private String createCellData(String data, Integer length, char ch) {
		String cellData = data;
		if (cellData == null) {
			cellData = "";
		}
		if (length == null) {
			return cellData;
		}
		if (cellData.length() > length) {
			return cellData.substring(0, length);
		}
		StringBuilder sb = new StringBuilder(cellData);
		while (sb.length() < length) {
			sb.append(ch);
		}
		return sb.toString();
	}

	private Locale getLocation() {
		return new Locale("ru");
	}
    
    private void writeLine(ByteBuffer buffer, String line) throws IOException {
        FileUtil.writeLine(buffer, line, null, MbParsingConstants.REGISTRY_FILE_CHARSET);
    }

    private void writeLine(ByteBuffer buffer, String[] cells, String delimiter) throws IOException {
        for (String cell : cells) {
            FileUtil.write(buffer, delimiter, null, MbParsingConstants.REGISTRY_FILE_CHARSET);
            FileUtil.write(buffer, cell, null, MbParsingConstants.REGISTRY_FILE_CHARSET);
        }
        FileUtil.writeLine(buffer, null, null, MbParsingConstants.REGISTRY_FILE_CHARSET);
    }

    private void writeCharToLine(ByteBuffer buffer, char ch, int count) throws IOException {
        FileUtil.writeCharToLine(buffer, ch, count, null, MbParsingConstants.REGISTRY_FILE_CHARSET);
    }

    private char mod31(int value) {
        if (value <= 0 || value > 31) {
            throw new IllegalArgumentException("The number must have range [1, 31]");
        }

        return value <= 9 ? String.valueOf(value).charAt(0) : (char)('A' + (value - 10));
    }

    private class FileProxyMessenger extends AbstractMessenger {

        private AbstractMessenger imessenger;
        private String fileName;

        private FileProxyMessenger(AbstractMessenger imessenger, String fileName) {
            this.imessenger = imessenger;
            this.fileName = fileName;
        }

        @Override
        public void addMessageInfo(String message, Object... parameters) {
            if (parameters == null || parameters.length == 0) {
                imessenger.addMessageInfo(message, fileName);
            } else if (parameters.length == 1) {
                imessenger.addMessageInfo(message, fileName, parameters[0]);
            } else if (parameters.length == 2) {
                imessenger.addMessageInfo(message, fileName, parameters[0], parameters[1]);
            } else if (parameters.length == 3) {
                imessenger.addMessageInfo(message, fileName, parameters[0], parameters[1], parameters[2]);
            }
        }

        @Override
        public void addMessageError(String message, Object... parameters) {
            if (parameters == null || parameters.length == 0) {
                imessenger.addMessageError(message, fileName);
            } else if (parameters.length == 1) {
                imessenger.addMessageError(message, fileName, parameters[0]);
            } else if (parameters.length == 2) {
                imessenger.addMessageError(message, fileName, parameters[0], parameters[1]);
            } else if (parameters.length == 3) {
                imessenger.addMessageError(message, fileName, parameters[0], parameters[1], parameters[2]);
            }
        }

        @Override
        public Queue<IMessage> getIMessages() {
            return imessenger.getIMessages();
        }

        @Override
        public int countIMessages() {
            return imessenger.countIMessages();
        }

        @Override
        public IMessage getNextIMessage() {
            return imessenger.getNextIMessage();
        }

        @Override
        public String getResourceBundle() {
            return null;
        }
    }

    private String getDataSource() {
        return configBean.getString(MbTransformerConfig.EIRC_DATA_SOURCE);
    }

}
