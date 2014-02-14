package ru.flexpay.eirc.registry.service.handle.exchange;

import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.util.DateUtil;
import ru.flexpay.eirc.registry.entity.Container;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public abstract class GeneralAccountOperation extends BaseAccountOperation {

    protected BaseAccountOperationData getContainerData(Container container) throws ContainerDataException {
        List<String> containerData = splitEscapableData(container.getData());
        if (containerData.size() < 2) {
            throw new ContainerDataException("Failed container format: {0}", container);
        }
        BaseAccountOperationData data = new BaseAccountOperationData();
        try {
            Date changeApplyingDate;
            String dateStr = containerData.get(1);
            if (StringUtils.isBlank(dateStr)) {
                changeApplyingDate = DateUtil.getCurrentDate();
            } else if (dateStr.length() == "ddMMyyyy".length()) {
                changeApplyingDate = new SimpleDateFormat("ddMMyyyy").parse(dateStr);
            } else if (dateStr.length() == "ddMMyyyyHHmmss".length()) {
                changeApplyingDate = new SimpleDateFormat("ddMMyyyyHHmmss").parse(dateStr);
            } else {
                changeApplyingDate = DateUtil.getCurrentDate();
            }
            if (DateUtil.getCurrentDate().before(changeApplyingDate)) {
                throw new ContainerDataException("Someone invented time machine? Specified date is in a future: {0}" + containerData.get(1));
            }
            data.setChangeApplyingDate(changeApplyingDate);
        } catch (ParseException e) {
            throw new ContainerDataException("Cannot parse date: {0}" + containerData.get(1));
        }

        if (containerData.size() >= 3) {
            data.setOldValue(containerData.get(2));
        }
        if (containerData.size() >= 4) {
            data.setNewValue(containerData.get(3));
        }
        return data;
    }

    protected class BaseAccountOperationData {

        private String oldValue;
        private String newValue;
        private Date changeApplyingDate;

        public String getOldValue() {
            return oldValue;
        }

        private void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        private void setNewValue(String newValue) {
            this.newValue = newValue;
        }

        public Date getChangeApplyingDate() {
            return changeApplyingDate;
        }

        private void setChangeApplyingDate(Date changeApplyingDate) {
            this.changeApplyingDate = changeApplyingDate;
        }
    }
}
