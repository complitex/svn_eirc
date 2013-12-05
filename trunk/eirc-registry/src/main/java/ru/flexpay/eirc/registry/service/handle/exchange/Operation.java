package ru.flexpay.eirc.registry.service.handle.exchange;

import org.apache.commons.lang.StringUtils;
import org.complitex.dictionary.service.exception.AbstractException;
import org.complitex.dictionary.util.DateUtil;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.util.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class Operation<T> {

	/**
	 * Symbol used escape special symbols
	 */
	public static final char ESCAPE_SYMBOL = '\\';

	/**
	 * Symbol used to split fields in containers
	 */
	public static final char CONTAINER_DATA_DELIMITER = ':';

    private Long code;

    private String oldValue;
    private String newValue;
    private Date changeApplyingDate;

    /**
     * Parse data and set operation id. Executing {@link Operation#prepareData}
     *
     * @param container Container
     * @throws AbstractException
     */
    protected Operation(Container container) throws AbstractException {
        List<String> containerData = splitEscapableData(container.getData());
        if (containerData.size() < 2) {
            throw new ContainerDataException("Failed container format: {}", container);
        }
        code = container.getType().getId();
        try {
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
        } catch (ParseException e) {
            throw new ContainerDataException("Cannot parse date: {0}" + containerData.get(1));
        }

        if (containerData.size() >= 3) {
            oldValue = containerData.get(2);
        }
        if (containerData.size() >= 4) {
            newValue = containerData.get(3);
        }
        prepareData(containerData);
    }

    public Long getCode() {
        return code;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Date getChangeApplyingDate() {
        return changeApplyingDate;
    }

    abstract public T getOldObject();

    abstract public T getNewObject();

    /**
	 * Handle operation.
	 *
     * @throws AbstractException if failure occurs
	 */
	abstract public void process() throws AbstractException;

	/**
	 * Handle operation.
	 *
	 * @param watchContext OperationWatchContext
	 * @throws AbstractException if failure occurs
	 */
	public final void process(OperationWatchContext watchContext)
			throws AbstractException {
		watchContext.before(this);
		try {
			process();
		} finally {
			watchContext.after(this);
		}
	}

    /**
     * Handle operation
     *
     * @param containerData Container data
     * @throws AbstractException
     */
    abstract protected void prepareData(List<String> containerData) throws AbstractException;

    /**
     * Split string with delimiter taking in account {@link Operation#ESCAPE_SYMBOL}
     *
     * @param containers Containers data
     * @return List of separate containers
     */
    private List<String> splitEscapableData(String containers) {
        return StringUtil.splitEscapable(containers, CONTAINER_DATA_DELIMITER, ESCAPE_SYMBOL);
    }

}
