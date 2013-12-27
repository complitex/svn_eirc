package ru.flexpay.eirc.mb_transformer.web.list;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.complitex.dictionary.web.component.AjaxFeedbackPanel;
import org.complitex.template.web.component.toolbar.ToolbarButton;
import org.complitex.template.web.component.toolbar.UploadButton;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.mb_transformer.service.MbCorrectionsFileConverter;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;

import javax.ejb.EJB;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class Transformer extends TemplatePage {

    private static final String IMAGE_AJAX_LOADER = "images/ajax-loader2.gif";


    private WebMarkupContainer container;

    @EJB
    private RegistryMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallback;

    @EJB
    private MbCorrectionsFileConverter mbCorrectionsFileConverter;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    public Transformer() throws ExecutionException, InterruptedException {
        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        final AjaxFeedbackPanel messages = new AjaxFeedbackPanel("messages");
        messages.setOutputMarkupId(true);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);
        container.add(messages);


        if (imessenger.countIMessages() > 0 || !finishCallback.isCompleted()) {
            initTimerBehavior();
        }

    }

    @Override
    protected List<? extends ToolbarButton> getToolbarButtons(String id) {

        return ImmutableList.of(
                new UploadButton(id, true) {

                    @Override
                    protected void onClick(final AjaxRequestTarget target) {

                        Transformer.this.initTimerBehavior();

                        try {
                            mbCorrectionsFileConverter.convert(imessenger, finishCallback);
                        } catch (ExecutionException e) {
                            log().error("Failed convert", e);
                        } finally {
                            showIMessages(target);
                        }
                    }
                }
        );
    }

    private void initTimerBehavior() {
        if (timerBehavior == null) {

            timerBehavior = new MessageBehavior(Duration.seconds(5));

            container.add(timerBehavior);
        }
    }

    private void showIMessages(AjaxRequestTarget target) {
        if (imessenger.countIMessages() > 0) {
            IMessenger.IMessage importMessage;

            while ((importMessage = imessenger.getNextIMessage()) != null) {
                switch (importMessage.getType()) {
                    case ERROR:
                        container.error(importMessage.getLocalizedString(getLocale()));
                        break;
                    case INFO:
                        container.info(importMessage.getLocalizedString(getLocale()));
                        break;
                }
            }
            target.add(container);
        }
    }

    private class MessageBehavior extends AjaxSelfUpdatingTimerBehavior {
        private MessageBehavior(Duration updateInterval) {
            super(updateInterval);
        }

        @Override
        protected void onPostProcessTarget(AjaxRequestTarget target) {
            showIMessages(target);

            if (finishCallback.isCompleted() && imessenger.countIMessages() <= 0) {
                stop();
                container.remove(timerBehavior);
                timerBehavior = null;
            }
        }
    }

}
