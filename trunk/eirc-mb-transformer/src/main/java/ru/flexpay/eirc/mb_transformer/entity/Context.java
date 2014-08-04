package ru.flexpay.eirc.mb_transformer.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.mb_transformer.service.MbConverterException;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.registry.service.AbstractMessenger;
import ru.flexpay.eirc.registry.util.FPRegistryConstants;
import ru.flexpay.eirc.service.correction.entity.ServiceCorrection;
import ru.flexpay.eirc.service.correction.service.ServiceCorrectionBean;
import ru.flexpay.eirc.service.entity.Service;
import ru.flexpay.eirc.service.service.ServiceBean;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavel Sknar
 */
public abstract class Context {
    private AbstractMessenger imessenger;
    private ServiceCorrectionBean serviceCorrectionBean;
    private ServiceBean serviceBean;
    private List<RegistryRecordData> registryRecords = Lists.newLinkedList();
    private String dataSource;

    private Long mbOrganizationId;
    private Long eircOrganizationId;
    private boolean skipHeader;

    private Registry registry;

    private Cache<String, String> serviceCache = CacheBuilder.newBuilder().
            maximumSize(1000).
            expireAfterWrite(10, TimeUnit.MINUTES).
            build();

    public Context(AbstractMessenger imessenger, ServiceCorrectionBean serviceCorrectionBean, ServiceBean serviceBean,
                   String dataSource,
                   Long mbOrganizationId, Long eircOrganizationId, boolean skipHeader, Registry registry) {
        this.imessenger = imessenger;
        this.mbOrganizationId = mbOrganizationId;
        this.eircOrganizationId = eircOrganizationId;
        this.skipHeader = skipHeader;
        this.serviceCorrectionBean = serviceCorrectionBean;
        this.serviceBean = serviceBean;
        this.dataSource = dataSource;
        this.registry = registry;
    }

    public AbstractMessenger getIMessenger() {
        return imessenger;
    }

    public Long getMbOrganizationId() {
        return mbOrganizationId;
    }

    public Long getEircOrganizationId() {
        return eircOrganizationId;
    }

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public Registry getRegistry() {
        return registry;
    }

    public RegistryRecordData getRegistryRecord(String[] fields, String serviceCode) throws MbConverterException {
        for (RegistryRecordData registryRecord : registryRecords) {
            if (!((RegistryRecordMapped)registryRecord).isUsing()) {
                ((RegistryRecordMapped) registryRecord).initData(fields, getInnerServiceCode(serviceCode));
                return registryRecord;
            }
        }
        RegistryRecordMapped registryRecord = getRegistryRecordInstance(fields, getInnerServiceCode(serviceCode));
        registryRecords.add(registryRecord);

        return registryRecord;
    }

    public boolean isValid() {
        return true;
    }

    public abstract String[] parseLine(String line);

    public abstract String getServiceCodes(String[] fields);

    public String getInnerServiceCode(String outServiceCode) throws MbConverterException {
        if (StringUtils.isEmpty(outServiceCode) || "0".equals(outServiceCode)) {
            return "0";
        }
        String serviceCode = serviceCache.getIfPresent(outServiceCode);
        if (serviceCode == null) {
            List<ServiceCorrection> serviceCorrections = serviceCorrectionBean.getServiceCorrections(dataSource,
                    FilterWrapper.of(new ServiceCorrection(null, null, outServiceCode, mbOrganizationId,
                            eircOrganizationId, null))
            );
            if (serviceCorrections.size() <= 0) {
                throw new MbConverterException(
                        "No found service correction with code {0}", outServiceCode);
            }
            if (serviceCorrections.size() > 1) {
                throw new MbConverterException("Found several correction for service with code {0}", outServiceCode);
            }
            Service service = serviceBean.getService(dataSource, serviceCorrections.get(0).getObjectId());
            serviceCode = service.getCode();
            serviceCache.put(outServiceCode, serviceCode);
        }
        return serviceCode;
    }

    protected abstract RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbConverterException, MbConverterException;

    protected void write(ByteBuffer buffer, String s) {
        buffer.put(getEncodingBytes(s));
    }

    protected void write(ByteBuffer buffer, long value) {
        buffer.put(getEncodingBytes(value));
    }

    private byte[] getEncodingBytes(String s) {
        return s.getBytes(FPRegistryConstants.EXPORT_FILE_CHARSET);
    }

    private byte[] getEncodingBytes(long value) {
        return getEncodingBytes(Long.toString(value));
    }
}
