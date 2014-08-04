package ru.flexpay.eirc.registry.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.complitex.dictionary.service.SessionBean;

import javax.ejb.EJB;
import java.util.Map;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
public abstract class IMessenger extends AbstractMessenger {

    @EJB
    private SessionBean sessionBean;

    private Map<Long, Queue<IMessage>> imessages = Maps.newConcurrentMap();

    public AbstractMessenger getInstance() {
        return new SimpleMessenger(getIMessages(), getResourceBundle());
    }

    @Override
    public Queue<IMessage> getIMessages() {
        return getUserIMessages();
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

    private static class SimpleMessenger extends AbstractMessenger {
        private Queue<IMessage> messages;
        private String resourceBundle;

        private SimpleMessenger(Queue<IMessage> messages, String resourceBundle) {
            this.messages = messages;
            this.resourceBundle = resourceBundle;
        }

        @Override
        public Queue<IMessage> getIMessages() {
            return messages;
        }

        @Override
        public String getResourceBundle() {
            return resourceBundle;
        }
    }
}
