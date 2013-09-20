package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.complitex.dictionary.service.SessionBean;

import javax.ejb.EJB;
import java.io.Serializable;
import java.util.Map;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
public class IMessenger {

    @EJB
    private SessionBean sessionBean;

    private Map<Long, Queue<IMessage>> imessages = Maps.newConcurrentMap();

    public void addMessageInfo(String message) {
        getIMessages().add(new IMessage(IMessageType.INFO, message));

    }

    public void addMessageError(String message) {
        getIMessages().add(new IMessage(IMessageType.ERROR, message));
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

    public class IMessage implements Serializable {
        private IMessageType type;

        private String data;

        private IMessage(IMessageType type, String data) {
            this.type = type;
            this.data = data;
        }

        public IMessageType getType() {
            return type;
        }

        public String getData() {
            return data;
        }
    }

    public enum IMessageType {
        ERROR, INFO
    }
}
