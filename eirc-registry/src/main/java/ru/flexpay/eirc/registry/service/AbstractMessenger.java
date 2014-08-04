package ru.flexpay.eirc.registry.service;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.complitex.dictionary.util.DateUtil;
import org.complitex.dictionary.util.ResourceUtil;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
public abstract class AbstractMessenger implements Serializable {

    private final static Map<Locale, IMessageConveyor> IMESSAGE_CONVEYOR =
            ImmutableMap.<Locale, IMessageConveyor>of(
                    new Locale("ru"), new MessageConveyor(new Locale("ru")),
                    Locale.ENGLISH, new MessageConveyor(Locale.ENGLISH)
            );

    private final static IMessageConveyor DEFAULT_IMESSAGE_CONVEYOR = IMESSAGE_CONVEYOR.get(0);

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss ");

    public void addMessageInfo(String message, Object... parameters) {
        addMessage(new IMessage(IMessageType.INFO, message, parameters));

    }

    public void addMessageError(String message, Object... parameters) {
        addMessage(new IMessage(IMessageType.ERROR, message, parameters));
    }

    public <E extends Enum<?>> void addMessageInfo(E message, Object... parameters) {
        addMessage(new IMessage(IMessageType.INFO, message, parameters));

    }

    public <E extends Enum<?>> void addMessageError(E message, Object... parameters) {
        addMessage(new IMessage(IMessageType.ERROR, message, parameters));
    }

    protected void addMessage(IMessage imessage) {
        getIMessages().add(imessage);
    }

    public abstract Queue<IMessage> getIMessages();

    public int countIMessages() {
        return getIMessages().size();
    }

    public IMessage getNextIMessage() {
        return getIMessages().poll();
    }

    public abstract String getResourceBundle();

    public class IMessage implements Serializable {

        private IMessageType type;

        private Date date = DateUtil.getCurrentDate();

        private String data = null;

        private Enum eData = null;

        private Object[] parameters;

        private IMessage(IMessageType type, String data, Object... parameters) {
            this.type = type;
            this.data = data;
            this.parameters = parameters;
        }

        private IMessage(IMessageType type, Date date, String data, Object... parameters) {
            this.type = type;
            this.date = date;
            this.data = data;
            this.parameters = parameters;
        }

        private IMessage(IMessageType type, Enum data, Object... parameters) {
            this.type = type;
            this.eData = data;
            this.parameters = parameters;
        }

        private IMessage(IMessageType type, Date date, Enum data, Object... parameters) {
            this.type = type;
            this.date = date;
            this.eData = data;
            this.parameters = parameters;
        }

        public IMessageType getType() {
            return type;
        }

        public Date getDate() {
            return date;
        }

        public String getData() {
            return data;
        }

        public Object[] getParameters() {
            return parameters;
        }

        public String getLocalizedString(Locale locale) {
            String message = "";
            if (data != null) {
                message = ResourceUtil.getString(getResourceBundle(), String.valueOf(getData()), locale);
                message = parameters != null && parameters.length > 0 ? MessageFormat.format(message, parameters) : message;
            }
            if (eData != null) {
                IMessageConveyor conveyor = IMESSAGE_CONVEYOR.get(locale);
                conveyor = conveyor == null? DEFAULT_IMESSAGE_CONVEYOR : conveyor;
                message = conveyor.getMessage(eData, parameters);
            }
            if (StringUtils.isNotEmpty(message)) {
                return TIME_FORMAT.format(date) + message;
            }
            return message;
        }

        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            builder.append(type).
                    append(data).
                    append(parameters);
            return builder.toString();
        }
    }

    public enum IMessageType {
        ERROR, INFO
    }
}
