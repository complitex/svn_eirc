package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import ru.flexpay.eirc.registry.web.list.RegistryList;

import javax.ejb.Stateful;
import java.util.Map;
import java.util.Queue;

/**
 * @author Pavel Sknar
 */
@Stateful
public class MessageContext {

    private Map<Class, Queue<RegistryList.ImportMessage>> messages = Maps.newConcurrentMap();

    public void add(Class clazz, RegistryList.ImportMessage message) {
        Queue<RegistryList.ImportMessage> queueMessages = messages.get(clazz);
        if (queueMessages == null) {
            queueMessages = Queues.newConcurrentLinkedQueue();
            messages.put(clazz, queueMessages);
        }
        queueMessages.add(message);
    }

    public RegistryList.ImportMessage poll(Class clazz) {
        Queue<RegistryList.ImportMessage> queueMessages = messages.get(clazz);
        if (queueMessages == null) {
            return null;
        }
        return queueMessages.poll();
    }

}
