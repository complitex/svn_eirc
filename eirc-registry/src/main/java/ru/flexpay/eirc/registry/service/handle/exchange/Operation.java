package ru.flexpay.eirc.registry.service.handle.exchange;

import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.Registry;
import ru.flexpay.eirc.registry.entity.RegistryRecordData;
import ru.flexpay.eirc.registry.util.StringUtil;

import java.util.List;

public abstract class Operation {

	/**
	 * Symbol used escape special symbols
	 */
	public static final char ESCAPE_SYMBOL = '\\';

	/**
	 * Symbol used to split fields in containers
	 */
	public static final char CONTAINER_DATA_DELIMITER = ':';

    /**
     * Parse data and set operation id
     *
     * @throws AbstractException
     */

    public abstract Long getCode();

    /**
	 * Handle operation.
	 *
     * @throws AbstractException if failure occurs
	 */
	public abstract void process(Registry registry, RegistryRecordData registryRecord, Container container,
                                 List<OperationResult> results) throws AbstractException;

	/**
	 * Handle operation.
	 *
	 * @param watchContext OperationWatchContext
	 * @throws AbstractException if failure occurs
	 */
	public void process(Registry registry, RegistryRecordData registryRecord, Container container,
                              List<OperationResult> results, OperationWatchContext watchContext)
			throws AbstractException {
		watchContext.before(this);
		try {
			process(registry, registryRecord, container, results);
		} finally {
			watchContext.after(this);
		}
	}

    /**
     * Split string with delimiter taking in account {@link Operation#ESCAPE_SYMBOL}
     *
     * @param containers Containers data
     * @return List of separate containers
     */
    protected List<String> splitEscapableData(String containers) {
        return StringUtil.splitEscapable(containers, CONTAINER_DATA_DELIMITER, ESCAPE_SYMBOL);
    }

}
