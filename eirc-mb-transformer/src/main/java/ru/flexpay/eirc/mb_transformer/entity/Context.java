package ru.flexpay.eirc.mb_transformer.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.complitex.dictionary.entity.FilterWrapper;
import ru.flexpay.eirc.mb_transformer.service.MbParseException;
import ru.flexpay.eirc.mb_transformer.util.FPRegistryConstants;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.service.entity.ServiceCorrection;
import ru.flexpay.eirc.service.service.ServiceCorrectionBean;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavel Sknar
 */
public abstract class Context {
    private IMessenger imessenger;
    private ServiceCorrectionBean serviceCorrectionBean;
    private List<RegistryRecordData> registryRecords = Lists.newLinkedList();

    private Long mbOrganizationId;
    private Long eircOrganizationId;
    private boolean skipHeader;

    private Registry registry;

    private Cache<String, String> serviceCache = CacheBuilder.newBuilder().
            maximumSize(1000).
            expireAfterWrite(10, TimeUnit.MINUTES).
            build();

    public Context(IMessenger imessenger, ServiceCorrectionBean serviceCorrectionBean,
                   Long mbOrganizationId, Long eircOrganizationId, boolean skipHeader, Registry registry) {
        this.imessenger = imessenger;
        this.mbOrganizationId = mbOrganizationId;
        this.eircOrganizationId = eircOrganizationId;
        this.skipHeader = skipHeader;
        this.serviceCorrectionBean = serviceCorrectionBean;
        this.registry = registry;
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

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public Registry getRegistry() {
        return registry;
    }

    public RegistryRecordData getRegistryRecord(String[] fields, String serviceCode) throws MbParseException {
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

    public abstract String[] parseLine(String line);

    public abstract String getServiceCodes(String[] fields);

    public String getInnerServiceCode(String outServiceCode) throws MbParseException {
        String serviceCode = serviceCache.getIfPresent(outServiceCode);
        if (serviceCode == null) {
            List<ServiceCorrection> serviceCorrections = serviceCorrectionBean.getServiceCorrections(
                    FilterWrapper.of(new ServiceCorrection(null, null, outServiceCode, mbOrganizationId,
                            registry.getSenderOrganizationId(), null))
            );
            if (serviceCorrections.size() <= 0) {
                throw new MbParseException(
                        "No found service correction with code {0}", outServiceCode);
            }
            if (serviceCorrections.size() > 1) {
                throw new MbParseException("Found several correction for service with code {0}", outServiceCode);
            }
            serviceCode = String.valueOf(serviceCorrections.get(0).getObjectId());
            serviceCache.put(outServiceCode, serviceCode);
        }
        return serviceCode;
    }

    protected abstract RegistryRecordMapped getRegistryRecordInstance(String[] fields, String serviceCode) throws MbParseException, MbParseException;

    protected void write(ByteBuffer buffer, String s) {
        buffer.put(getEncodingBytes(s));
    }

    protected void write(ByteBuffer buffer, long value) {
        buffer.put(getEncodingBytes(value));
    }

    private byte[] getEncodingBytes(String s) {
        return s.getBytes(Charset.forName(FPRegistryConstants.EXPORT_FILE_ENCODING));
    }

    private byte[] getEncodingBytes(long value) {
        return getEncodingBytes(Long.toString(value));
    }
}
