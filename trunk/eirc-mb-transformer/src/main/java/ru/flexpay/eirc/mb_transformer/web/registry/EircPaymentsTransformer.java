package ru.flexpay.eirc.mb_transformer.web.registry;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;
import ru.flexpay.eirc.mb_transformer.service.EircPaymentsRegistryConverter;
import ru.flexpay.eirc.mb_transformer.service.FileService;
import ru.flexpay.eirc.mb_transformer.service.MbTransformerConfigBean;
import ru.flexpay.eirc.registry.service.AbstractFinishCallback;
import ru.flexpay.eirc.registry.service.AbstractMessenger;
import ru.flexpay.eirc.registry.service.RegistryFinishCallback;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.parse.FileReader;
import ru.flexpay.eirc.registry.web.component.BrowserFilesDialog;
import ru.flexpay.eirc.registry.web.component.IMessengerContainer;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
public class EircPaymentsTransformer extends TemplatePage {

    private IMessengerContainer container;

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

        container = new IMessengerContainer("container", imessenger, finishCallback);
        container.setOutputMarkupPlaceholderTag(true);
        container.setVisible(true);
        add(container);
        container.add(messages);

        final BrowserFilesDialog paymentsDialog = new BrowserFilesDialog("paymentsDialog", container, paymentsFileName,
                fileService.getWorkDir());
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
                boolean error = false;
                File eircFile = paymentsFileName.getObject();
                if (eircFile == null) {
                    error = true;
                    container.error(getString("required_eirc_file"));
                }
                String workDir = fileService.getWorkDir();
                if (workDir == null) {
                    error = true;
                    container.error(getString("undefined_work_dir"));
                }
                if (error) {
                    target.add(container);
                    return;
                }

                EircPaymentsTransformer.this.initTimerBehavior();

                try {
                    Long mbOrganizationId = configBean.getInteger(MbTransformerConfig.MB_ORGANIZATION_ID, true).longValue();
                    Long eircOrganizationId = configBean.getInteger(MbTransformerConfig.EIRC_ORGANIZATION_ID, true).longValue();

                    FileReader reader = new FileReader(new FileInputStream(eircFile), eircFile.getName(), eircFile.length());

                    eircPaymentsRegistryConverter.exportToMegaBank(reader, workDir, null, mbOrganizationId,
                            eircOrganizationId, imessenger, finishCallback);
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
        container.initTimerBehavior();
    }

    private void showIMessages(AjaxRequestTarget target) {
        container.showIMessages(target);
    }
}
