package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.complitex.dictionary.service.SessionBean;
import org.complitex.dictionary.util.ResourceUtil;

import javax.ejb.EJB;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
public abstract class IMessenger {

    @EJB
    private SessionBean sessionBean;

    private Map<Long, Queue<IMessage>> imessages = Maps.newConcurrentMap();

    public void addMessageInfo(String message, Object... parameters) {
        getIMessages().add(new IMessage(IMessageType.INFO, message, parameters));

    }

    public void addMessageError(String message, Object... parameters) {
        getIMessages().add(new IMessage(IMessageType.ERROR, message, parameters));
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

    protected abstract String getResourceBundle();

    public class IMessage implements Serializable {
        private IMessageType type;

        private String data;

        private Object[] parameters;

        private IMessage(IMessageType type, String data, Object... parameters) {
            this.type = type;
            this.data = data;
            this.parameters = parameters;
        }

        public IMessageType getType() {
            return type;
        }

        public String getData() {
            return data;
        }

        public Object[] getParameters() {
            return parameters;
        }

        public String getLocalizedString(Locale locale) {
            String message = ResourceUtil.getString(getResourceBundle(), String.valueOf(getData()), locale);
            return parameters != null && parameters.length > 0? MessageFormat.format(message, parameters) : message;
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
