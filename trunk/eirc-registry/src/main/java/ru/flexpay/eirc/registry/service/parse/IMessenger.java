package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Queues;

import java.io.Serializable;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
public class IMessenger {

    private Queue<IMessage> imessages = Queues.newConcurrentLinkedQueue();

    public void addMessageInfo(String message) {
        imessages.add(new IMessage(IMessageType.INFO, message));

    }

    public void addMessageError(String message) {
        imessages.add(new IMessage(IMessageType.ERROR, message));
    }


    public Queue<IMessage> getIMessages() {
        return imessages;
    }

    public int countIMessages() {
        return imessages.size();
    }

    public IMessage getNextIMessage() {
        return imessages.poll();
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
