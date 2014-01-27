package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.complitex.dictionary.service.SessionBean;
import org.complitex.dictionary.util.DateUtil;
import org.complitex.dictionary.util.ResourceUtil;

import javax.ejb.EJB;
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
public abstract class IMessenger {

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss ");

    @EJB
    private SessionBean sessionBean;

    private Map<Long, Queue<IMessage>> imessages = Maps.newConcurrentMap();

    public IMessenger getInstance() {
        return new IMessenger() {

            Queue<IMessage> userIMessages = getUserIMessages();

            @Override
            public IMessenger getInstance() {
                return this;
            }

            @Override
            public Queue<IMessage> getIMessages() {
                return userIMessages;
            }

            @Override
            protected String getResourceBundle() {
                return IMessenger.this.getResourceBundle();
            }
        };
    }

    public void addMessageInfo(String message, Object... parameters) {
        addMessage(new IMessage(IMessageType.INFO, message, parameters));

    }

    public void addMessageError(String message, Object... parameters) {
        addMessage(new IMessage(IMessageType.ERROR, message, parameters));
    }

    protected void addMessage(IMessage imessage) {
        getIMessages().add(imessage);
    }

    public Queue<IMessage> getIMessages() {
        return getUserIMessages();
    }

    public int countIMessages() {
        return getIMessages().size();
    }

    public IMessage getNextIMessage() {
        return getIMessages().poll();
    }

    private Queue<IMessage> getUserIMessages() {
        Long userId = sessionBean.getCurrentUserId();
        Queue<IMessage> userIMessages = imessages.get(userId);
        if (userIMessages == null) {
            userIMessages = Queues.newConcurrentLinkedQueue();
            imessages.put(userId, userIMessages);
        }
        return userIMessages;
    }

    private String getInnerResourceBundle() {
        return getResourceBundle();
    }

    protected abstract String getResourceBundle();

    public class IMessage implements Serializable {

        private IMessageType type;

        private Date date = DateUtil.getCurrentDate();

        private String data;

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
            String message = ResourceUtil.getString(getResourceBundle(), String.valueOf(getData()), locale);
            message = parameters != null && parameters.length > 0? MessageFormat.format(message, parameters) : message;

            return TIME_FORMAT.format(date) + message;
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
