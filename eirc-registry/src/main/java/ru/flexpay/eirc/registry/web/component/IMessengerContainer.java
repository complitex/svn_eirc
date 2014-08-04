package ru.flexpay.eirc.registry.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.AbstractMessenger;
import ru.flexpay.eirc.registry.service.IMessenger;

/**
 * @author Pavel Sknar
 */
public class IMessengerContainer extends WebMarkupContainer {
    private AbstractMessenger imessenger;
    private AbstractFinishCallback finishCallback;
    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    public IMessengerContainer(String id, AbstractMessenger imessenger, AbstractFinishCallback finishCallback) {
        super(id);
        this.imessenger = imessenger;
        this.finishCallback = finishCallback;
        initTimerBehavior(false);
    }

    public IMessengerContainer(String id, IModel<?> model, AbstractMessenger imessenger, AbstractFinishCallback finishCallback) {
        super(id, model);
        this.imessenger = imessenger;
        this.finishCallback = finishCallback;
        initTimerBehavior(false);
    }

    /**
     * Show all messages and add component to target
     *
     * @param target Ajax request target
     */
    public void showIMessages(AjaxRequestTarget target) {
        if (imessenger.countIMessages() > 0) {
            IMessenger.IMessage importMessage;

            while ((importMessage = imessenger.getNextIMessage()) != null) {
                switch (importMessage.getType()) {
                    case ERROR:
                        error(importMessage.getLocalizedString(getLocale()));
                        break;
                    case INFO:
                        info(importMessage.getLocalizedString(getLocale()));
                        break;
                }
            }
            target.add(this);
        }
    }

    public void initTimerBehavior() {
        initTimerBehavior(true);
    }

    public void initTimerBehavior(boolean force) {
        if (!force && isCompleted()) {
            return;
        }
        if (timerBehavior == null) {

            timerBehavior = new MessageBehavior(Duration.seconds(5));

            add(timerBehavior);
        }
    }

    private class MessageBehavior extends AjaxSelfUpdatingTimerBehavior {
        private MessageBehavior(Duration updateInterval) {
            super(updateInterval);
        }

        @Override
        protected void onPostProcessTarget(AjaxRequestTarget target) {
            showIMessages(target);

            if (isCompleted()) {
                stop(target);
                remove(timerBehavior);
                timerBehavior = null;
            }
        }
    }

    protected boolean isCompleted() {
        return finishCallback.isCompleted() && imessenger.countIMessages() <= 0;
    }
}
