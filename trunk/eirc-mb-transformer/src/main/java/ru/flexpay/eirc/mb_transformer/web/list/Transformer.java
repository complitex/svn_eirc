package ru.flexpay.eirc.mb_transformer.web.list;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.complitex.dictionary.web.component.ajax.AjaxFeedbackPanel;
import org.complitex.template.web.security.SecurityRole;
import org.complitex.template.web.template.TemplatePage;
import ru.flexpay.eirc.mb_transformer.entity.MbFile;
import ru.flexpay.eirc.mb_transformer.entity.MbTransformerConfig;
import ru.flexpay.eirc.mb_transformer.service.MbCorrectionsFileConverter;
import ru.flexpay.eirc.mb_transformer.service.MbTransformerConfigBean;
import ru.flexpay.eirc.registry.service.FinishCallback;
import ru.flexpay.eirc.registry.service.IMessenger;
import ru.flexpay.eirc.registry.service.RegistryMessenger;
import ru.flexpay.eirc.registry.service.parse.RegistryFinishCallback;
import ru.flexpay.eirc.service.entity.Service;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavel Sknar
 */
@AuthorizeInstantiation(SecurityRole.AUTHORIZED)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class Transformer extends TemplatePage {

    private static final String IMAGE_AJAX_LOADER = "images/ajax-loader2.gif";


    private WebMarkupContainer container;

    @EJB
    private RegistryMessenger imessengerService;

    private IMessenger imessenger;

    @EJB
    private RegistryFinishCallback finishCallbackService;

    private FinishCallback finishCallback;

    @EJB
    private MbCorrectionsFileConverter mbCorrectionsFileConverter;

    @EJB(name = "MbTransformerConfigBean")
    private MbTransformerConfigBean configBean;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    private String correctionsFileName;
    private String chargesFileName;
    private String resultFileName;

    public Transformer() throws ExecutionException, InterruptedException {

        imessenger = imessengerService.getInstance();
        finishCallback = finishCallbackService.getInstance();

        init();
    }

    private void init() throws ExecutionException, InterruptedException {
        IModel<String> labelModel = new ResourceModel("label");

        add(new Label("title", labelModel));
        add(new Label("label", labelModel));

        Form<Service> form = new Form<>("form");
        add(form);

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

        DropDownChoice<String> chargesFiles = new DropDownChoice<>("charges", new IModel<String>() {
            @Override
            public String getObject() {
                return chargesFileName;
            }

            @Override
            public void setObject(String object) {
                chargesFileName = object;
            }

            @Override
            public void detach() {

            }
        }, getFileList());
        form.add(chargesFiles);

        DropDownChoice<String> correctionsFiles = new DropDownChoice<>("corrections", new IModel<String>() {
            @Override
            public String getObject() {
                return correctionsFileName;
            }

            @Override
            public void setObject(String object) {
                correctionsFileName = object;
            }

            @Override
            public void detach() {

            }
        }, getFileList());
        form.add(correctionsFiles);

        TextField<String> resultFile = new TextField<>("result", new IModel<String>() {
            @Override
            public String getObject() {
                return resultFileName;
            }

            @Override
            public void setObject(String object) {
                resultFileName = object;
            }

            @Override
            public void detach() {

            }
        });
        form.add(resultFile);

        AjaxButton transform = new AjaxButton("transform") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                Transformer.this.initTimerBehavior();

                try {
                    Long mbOrganizationId = configBean.getInteger(MbTransformerConfig.MB_ORGANIZATION_ID, true).longValue();
                    Long eircOrganizationId = configBean.getInteger(MbTransformerConfig.EIRC_ORGANIZATION_ID, true).longValue();
                    String tmpDir = configBean.getString(MbTransformerConfig.TMP_DIR);
                    if (!isDirectory(tmpDir)) {
                        log().error("Is not directory {}={}", MbTransformerConfig.TMP_DIR.name(), tmpDir);
                        return;
                    }

                    MbFile correctionsFile = getMbFile(correctionsFileName);
                    MbFile chargesFile = getMbFile(chargesFileName);
                    if (correctionsFile == null || chargesFile == null) {
                        return;
                    }

                    //mbCorrectionsFileConverter.convert(imessenger, finishCallback);
                    mbCorrectionsFileConverter.convertFile(correctionsFile, chargesFile, getWorkDir(),
                            resultFileName, tmpDir, mbOrganizationId, eircOrganizationId,
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
                stop();
                container.remove(timerBehavior);
                timerBehavior = null;
            }
        }
    }

    public List<String> getFileList(){
        String dir = getWorkDir();
        String[] files = new File(dir).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return true;//name.toLowerCase().contains("." + extension);
            }
        });
        return Arrays.asList(files);
    }

    public String getWorkDir() {
        String workDir = configBean.getString(MbTransformerConfig.WORK_DIR);
        if (!isDirectory(workDir)) {
            log().error("Is not directory {}={}", MbTransformerConfig.TMP_DIR.name(), workDir);
            return null;
        }
        return workDir;
    }

    public Long getLong(Properties transformerProperties, String propertyName) {
        String value = transformerProperties.getProperty(propertyName);
        if (value == null) {
            log().error("Config did not content {}", propertyName);
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log().error("{} have failed number format", value);
        }
        return null;
    }

    private MbFile getMbFile(String filename) throws FileNotFoundException {
        String workDir = getWorkDir();
        File file = new File(workDir, filename);
        if (!file.isFile()) {
            log().error("{} is not file", file.getPath());
            return null;
        }
        return new MbFile(workDir, filename);
    }

    public static boolean isDirectory(String tmpDirPath) {
        File tmpDir = new File(tmpDirPath);
        return tmpDir.exists() && tmpDir.isDirectory();
    }

}
