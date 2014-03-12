package ru.flexpay.eirc.mb_transformer.web.registry;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;
import ru.flexpay.eirc.mb_transformer.service.EircPaymentsRegistryConverter;
import ru.flexpay.eirc.mb_transformer.service.FileService;
import ru.flexpay.eirc.mb_transformer.service.MbTransformerConfigBean;
import ru.flexpay.eirc.mb_transformer.web.component.BrowserFilesDialog;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.handle.AbstractMessenger;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;
import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircPaymentsTransformer extends TemplatePage {

    private WebMarkupContainer container;

    @EJB
    private RegistryMessenger imessengerService;

    private AbstractMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallbackService;

    private AbstractFinishCallback finishCallback;

    @EJB
    private EircPaymentsRegistryConverter eircPaymentsRegistryConverter;

    @EJB(name = "MbTransformerConfigBean")
    private MbTransformerConfigBean configBean;

    @EJB
    private FileService fileService;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    private Model<File> paymentsFileName = new Model<>();

    public EircPaymentsTransformer() throws ExecutionException, InterruptedException {

        imessenger = imessengerService.getInstance();
        finishCallback = finishCallbackService.getInstance();

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

        final BrowserFilesDialog paymentsDialog = new BrowserFilesDialog("paymentsDialog", container, paymentsFileName);
        add(paymentsDialog);

        Form<Service> form = new Form<>("form");
        container.add(form);

        AjaxButton paymentsButton = new AjaxButton("paymentsButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                paymentsDialog.open(target);
            }
        };
        form.add(paymentsButton);

        TextField<String> paymentsFile = new TextField<>("paymentsFile", new IModel<String>() {
            @Override
            public String getObject() {
                File paymentsFile = EircPaymentsTransformer.this.paymentsFileName.getObject();
                return paymentsFile == null? "" : paymentsFile.getName();
            }

            @Override
            public void setObject(String object) {
            }

            @Override
            public void detach() {

            }
        });
        paymentsFile.setEnabled(false);
        form.add(paymentsFile);

        AjaxButton transform = new AjaxButton("transform") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                EircPaymentsTransformer.this.initTimerBehavior();

                try {
                    Long mbOrganizationId = configBean.getInteger(MbTransformerConfig.MB_ORGANIZATION_ID, true).longValue();
                    Long eircOrganizationId = configBean.getInteger(MbTransformerConfig.EIRC_ORGANIZATION_ID, true).longValue();
                    String tmpDir = fileService.getTmpDir();
                    if (tmpDir == null) {
                        container.error("undefined_tmp_dir");
                        return;
                    }
                    String workDir = fileService.getWorkDir();
                    if (workDir == null) {
                        container.error("undefined_work_dir");
                        return;
                    }

                    eircPaymentsRegistryConverter.exportToMegaBank(paymentsFileName.getObject(), workDir, null,
                            mbOrganizationId, eircOrganizationId, null, tmpDir,
                            imessenger, finishCallback);
                } catch (Exception e) {
                    log().error("Failed convert", e);
                } finally {
                    showIMessages(target);
                }
            }
        };
        form.add(transform);

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
                stop(target);
                container.remove(timerBehavior);
                timerBehavior = null;
            }
        }
    }
}
