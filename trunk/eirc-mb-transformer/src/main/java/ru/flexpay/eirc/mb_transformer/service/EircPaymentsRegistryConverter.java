package ru.flexpay.eirc.mb_transformer.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.complitex.correction.entity.OrganizationCorrection;
import org.complitex.correction.service.OrganizationCorrectionBean;
import org.complitex.dictionary.entity.FilterWrapper;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.service.executor.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.mb_transformer.util.MbParsingConstants;
import ru.flexpay.eirc.organization.entity.Organization;
import ru.flexpay.eirc.organization.strategy.EircOrganizationStrategy;
import ru.flexpay.eirc.registry.entity.*;
import ru.flexpay.eirc.registry.service.file.RSASignatureService;
import ru.flexpay.eirc.registry.service.parse.FileReader;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;
import ru.flexpay.eirc.registry.service.parse.RegistryFormatException;
import ru.flexpay.eirc.registry.util.FileUtil;
import ru.flexpay.eirc.registry.util.ParseUtil;
import ru.flexpay.eirc.registry.util.StringUtil;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.entity.ServiceCorrection;
import ru.flexpay.eirc.service.service.ServiceBean;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.Signature;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants.*;


/**
 * Generate the payments registry in MB format.
 */
@Singleton
public class EircPaymentsRegistryConverter {

	private static final Logger log = LoggerFactory.getLogger(EircPaymentsRegistryConverter.class.getClass());

    private static final int NUMBER_READ_CHARS = 10000;

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	private static final SimpleDateFormat paymentDateFormat = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat paymentPeriodDateFormat = new SimpleDateFormat("yyyyMM");

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
	private static final Map<String, String> SERVICE_NAMES = new HashMap<String, String>();

	static {
		SERVICE_NAMES.put("01", "ЭЛЕКТР  ");
		SERVICE_NAMES.put("02", "КВ/ЭКСПЛ"); // точно известно
		SERVICE_NAMES.put("03", "ОТОПЛ   ");
		SERVICE_NAMES.put("04", "ГОР ВОДА");
		SERVICE_NAMES.put("05", "ХОЛ ВОДА");
		SERVICE_NAMES.put("06", "КАНАЛИЗ ");
		SERVICE_NAMES.put("07", "ГАЗ ВАР ");
		SERVICE_NAMES.put("08", "ГАЗ ОТОП");
		SERVICE_NAMES.put("09", "РАДИО   ");
		SERVICE_NAMES.put("10", "АНТ     "); // точно известно
		SERVICE_NAMES.put("11", "ЖИВ     "); // точно известно
		SERVICE_NAMES.put("12", "ГАРАЖ   "); // точно известно
		SERVICE_NAMES.put("13", "ПОГРЕБ  "); // точно известно
		SERVICE_NAMES.put("14", "САРАЙ   "); // точно известно
		SERVICE_NAMES.put("15", "КЛАДОВКА"); // точно известно
		SERVICE_NAMES.put("16", "ТЕЛЕФОН ");
		SERVICE_NAMES.put("19", "АССЕНИЗ ");
		SERVICE_NAMES.put("20", "ЛИФТ    ");
		SERVICE_NAMES.put("21", "ХОЗ РАСХ"); // точно известно
		SERVICE_NAMES.put("22", "НАЛ ЗЕМЛ");
		SERVICE_NAMES.put("23", "ПОВ ПОДК");
		SERVICE_NAMES.put("24", "ОПЛ АКТ ");
		SERVICE_NAMES.put("25", "РЕМ СЧЁТ");
	}

    private static byte[] DELIMITER = "|".getBytes(MbParsingConstants.REGISTRY_FILE_CHARSET);

    @EJB
    private EircOrganizationStrategy eircOrganizationStrategy;

    @EJB
    private OrganizationCorrectionBean organizationCorrectionBean;

    @EJB
    private ServiceCorrectionBean serviceCorrectionBean;

    @EJB
    private ServiceBean serviceBean;

    @EJB
    private RSASignatureService signatureService;

    private Cache<String, String> serviceCache = CacheBuilder.newBuilder().
            maximumSize(1000).
            expireAfterWrite(10, TimeUnit.MINUTES).
            build();

    public void exportToMegaBank(File eircFile, String dir, String mbFileName, Long mbOrganizationId, Long eircOrganizationId,
                                 String privateKey, String tmpDir) throws IOException,
            RegistryFormatException, ExecuteException {
        final FileReader reader = new FileReader(new FileInputStream(eircFile));
        try {
            DataSource dataSource = new DataSource() {
                List<FileReader.Message> listMessage = Lists.newArrayList();
                int idx = 0;

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
                        log.error("{}", e.toString());
                    }
                    if (message == null) {
                        return null;
                    }
                    if (message.getType() != MESSAGE_TYPE_HEADER) {
                        log.error("Failed registry format: wanted header");
                        return null;
                    }
                    registry = new Registry();
                    List<String> listMessage = parseMessage(message.getBody());
                    try {
                        ParseUtil.fillUpRegistry(listMessage, ParseRegistryConstants.HEADER_DATE_FORMAT, registry);
                        return registry;
                    } catch (RegistryFormatException e) {
                        log.error("{}", e.toString());
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
                        log.info("Find footer. End of file");
                        return null;
                    }
                    if (message.getType() != MESSAGE_TYPE_RECORD) {
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
                    return listMessage.get(idx++);
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

            Organization serviceProvider = eircOrganizationStrategy.findById(registry.getRecipientOrganizationId(), true);

            if (serviceProvider == null) {
                log.error("Service provider {} not found", registry.getRecipientOrganizationId());
                return;
            }

            try {
                Signature signature = getPrivateSignature(privateKey);
                exportToMegaBank(dataSource, serviceProvider, mbOrganizationId, eircOrganizationId, signature, dir,
                        tmpDir, mbFileName, eircFile.length());
            } catch (Exception e) {
                log.error("{}", e.toString());
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
                                     Signature signature, String dir, String tmpDir, String mbFileName, long mbFileLength)
            throws ExecutionException, AbstractException {

        Registry registry = dataSource.getRegistry();

        FileChannel rwChannel = null;
        FileChannel outChannel = null;

        File tmpFile = new File(tmpDir, mbFileName + "_tmp_" + new Date().getTime());
        
		try {

			String externalServiceProviderId = getExternalServiceProviderId(registry, serviceProviderOrganization,
                    mbOrganizationId, eircOrganizationId);

            rwChannel = new RandomAccessFile(tmpFile, "rw").getChannel();
            ByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, mbFileLength);


			// заголовочные строки
			writeLine(buffer, "\tРеестр поступивших платежей. Мемориальный ордер №" + registry.getId());
			writeLine(buffer, "\tДля \"" + eircOrganizationStrategy.displayDomainObject(serviceProviderOrganization, getLocation()) + "\". День распределения платежей " +
						 dateFormat.format(new Date()) + ".");
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
                writeInfoLine(buffer, "|", registryRecord, serviceProviderOrganization.getId(), eircOrganizationId);
			}

            buffer.flip();

            signature.update(buffer);

            buffer.flip();

            byte[] sign = signature.sign();

			// служебные строки

			log.info("Writing service lines");

            outChannel = new FileOutputStream(new File(dir, mbFileName)).getChannel();
            ByteBuffer buff = ByteBuffer.allocateDirect(32 * 1024);

            writeCharToLine(buff, '_', 128);
            writeDigitalSignature(buff, sign);
            writeCharToLine(buff, '_', 128);

            buff.flip();
            outChannel.write(buff);
            buff.clear();

            buffer.flip();
            outChannel.write(buffer);
            buffer.clear();

		} catch (IOException | SignatureException e) {
			throw new ExecutionException(e);
		} finally {
            IOUtils.closeQuietly(rwChannel);
            IOUtils.closeQuietly(outChannel);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
		}
	}

	private String getExternalServiceProviderId(Registry registry, Organization serviceProviderOrganization,
                                                Long mbOrganizationId, Long eircOrganizationId) throws MbConverterException {

        List<OrganizationCorrection> organizationCorrections = organizationCorrectionBean.getOrganizationCorrections(
            FilterWrapper.of(new OrganizationCorrection(null, serviceProviderOrganization.getId(), null,
                    mbOrganizationId, eircOrganizationId, null)));
        if (organizationCorrections.size() <= 0) {
            throw new MbConverterException("No service provider correction with id {0}", serviceProviderOrganization.getId());
        }
        if (organizationCorrections.size() > 1) {
            throw new MbConverterException("Found several correction for service provider {0}", serviceProviderOrganization.getId());
        }
        return organizationCorrections.get(0).getCorrection();
	}

    public String getOutServiceCode(String innerServiceCode, Long serviceProviderId, Long eircOrganizationId) throws MbConverterException {
        String serviceCode = serviceCache.getIfPresent(innerServiceCode);
        if (serviceCode != null) {
            return serviceCode;
        }

        List<Service> services = serviceBean.getServices(FilterWrapper.of(new Service(innerServiceCode)));
        if (services.size() == 0) {
            throw new MbConverterException(
                    "No found service with code {0}", innerServiceCode);
        }
        if (services.size() > 1) {
            throw new MbConverterException("Found several services with code {0}", innerServiceCode);
        }
        List<ServiceCorrection> serviceCorrections = serviceCorrectionBean.getServiceCorrections(
                FilterWrapper.of(new ServiceCorrection(null, services.get(0).getId(), null, serviceProviderId,
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
        serviceCache.put(innerServiceCode, serviceCode);

        return serviceCode;
    }

	private void writeInfoLine(ByteBuffer buffer, String delimeter, RegistryRecordData record,
                               Long serviceProviderId, Long eircOrganizationId)
            throws ExecutionException, MbConverterException {

		//граница таблицы
		writeCellData(buffer, DELIMITER, "", 0, ' ');

		// код квитанции
		writeCellData(buffer, DELIMITER, String.valueOf(record.getUniqueOperationNumber()), TABLE_HEADERS[0].length(), ' ');

		// лиц. счёт ЕРЦ
		String eircAccount = null;
		//List<RegistryRecordContainer> containers = registryRecordService.getRecordContainers(record);
		for (Container container : record.getContainers()) {
			if (container.getData() != null && container.getData().startsWith("15:")) {
				String[] containerFields = container.getData().split(":");
				if (containerFields.length >= 4) {
					eircAccount = containerFields[3];
					break;
				}
			}
		}
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
		serviceCode = getOutServiceCode(serviceCode, serviceProviderId, eircOrganizationId);
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
		String paymentDate = operationDate != null ? paymentDateFormat.format(operationDate) : null;
		writeCellData(buffer, DELIMITER, paymentDate, TABLE_HEADERS[12].length(), ' ');

		// с какого месяца оплачена услуга
		String paymentMounth = null;
		if (operationDate != null) {
			Calendar cal = (Calendar) Calendar.getInstance().clone();
			cal.setTime(operationDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.roll(Calendar.MONTH, -1);
			paymentMounth = paymentPeriodDateFormat.format(cal.getTime());
		}
		writeCellData(buffer, DELIMITER, paymentMounth, TABLE_HEADERS[13].length(), ' ');

		// по какой месяц оплачена услуга
		writeCellData(buffer, DELIMITER, paymentMounth, TABLE_HEADERS[14].length(), ' ');

		// сумма (значение суммы изначально передаётся в рублях, но должно быть записано в копейках)\
		int sum = record.getAmount().multiply(new BigDecimal("100")).intValue();
		writeCellData(buffer, DELIMITER, String.valueOf(sum), null, ' ');

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

	private void writeDigitalSignature(ByteBuffer buffer, byte[] sign) throws IOException {
		if (sign != null) {
			String str = new String(sign, MbParsingConstants.REGISTRY_FILE_CHARSET);
            writeLine(buffer, str);
            int nLineFeeds = StringUtils.countMatches(str, "\n") + 1; // one added in writeLine(buffer, sign);
			while (nLineFeeds < 2) {
				writeLine(buffer, "");
				++nLineFeeds;
			}
		} else {
			writeLine(buffer, "");
			writeLine(buffer, "");
		}
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

}
