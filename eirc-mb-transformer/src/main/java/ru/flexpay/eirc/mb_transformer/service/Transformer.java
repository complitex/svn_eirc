package ru.flexpay.eirc.mb_transformer.service;

import org.apache.commons.cli.*;
import org.complitex.dictionary.service.exception.AbstractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.mb_transformer.entity.MbFile;
import ru.flexpay.eirc.registry.service.RegistryMessenger;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Pavel Sknar
 */
public class Transformer {

    private static final Logger log = LoggerFactory.getLogger(Transformer.class);

    private static final Locale LOCALE = new Locale("ru");

    private static final String PROPERTIES_FILE = "config.properties";

    private static final String MB_ORGANIZATION_ID = "mbOrganizationId";
    private static final String TMP_DIR = "tmpDir";

    public static void main(String... args) throws ParseException {

        Options options = new Options();

        Option optionCorrections = new Option("c", "corrections", true, "Input MB corrections pathname");
        optionCorrections.setRequired(true);
        options.addOption(optionCorrections);

        Option optionCharges = new Option("n", "charges", true, "Input MB charges pathname");
        optionCharges.setRequired(true);
        options.addOption(optionCharges);

        Option registryOption = new Option("r", "registry", true, "Output EIRC registry pathname");
        registryOption.setRequired(true);
        options.addOption(registryOption);

        CommandLineParser commandLineParser = new PosixParser();
        CommandLine commandLine = commandLineParser.parse(options, args);

        String correctionsFileName = commandLine.getOptionValue('c');
        String chargesFileName = commandLine.getOptionValue('n');
        String registryFileName = commandLine.getOptionValue('r');

        File registryFile = new File(registryFileName);
        if (registryFile.exists()) {
            log.error("Registry file {} already exists", registryFileName);
            return;
        }

        java.util.Properties transformerProperties = new Properties();
        try {
            transformerProperties.load(new FileInputStream(new File(PROPERTIES_FILE)));
        } catch (IOException e) {
            log.error("Can not read properties", e);
            return;
        }
        Long mbOrganizationId = getMbOrganizationId(transformerProperties);
        if (mbOrganizationId == null) {
            return;
        }
        String tmpDir = transformerProperties.getProperty(TMP_DIR);
        if (!isDirectory(tmpDir)) {
            log.error("Failed properties {}={}", TMP_DIR, tmpDir);
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        // Use the MODULES property to specify the set of modules to be initialized,
        // in this case a java.io.File
        properties.put(EJBContainer.MODULES, new File("build/jar"));

        EJBContainer ec = EJBContainer.createEJBContainer(properties);
        Context ctx = ec.getContext();

        try {
            RegistryMessenger messenger = new RegistryMessenger() {
                @Override
                protected void addMessage(IMessage imessage) {
                    switch (imessage.getType()) {
                        case ERROR:
                            log.error(imessage.getLocalizedString(LOCALE));
                            break;
                        case INFO:
                            log.info(imessage.getLocalizedString(LOCALE));
                            break;
                    }
                }
            };
            //RegistryFinishCallback callback = (RegistryFinishCallback) ctx.lookup("java:global/eirc-mb-transformer-1.0.0-SNAPSHOT-jar-with-dependencies/RegistryFinishCallback");
            MbCorrectionsFileConverter converter = (MbCorrectionsFileConverter) ctx.lookup("java:global/eirc-mb-transformer-1.0.0-SNAPSHOT-jar-with-dependencies/MbCorrectionsFileConverter");

            MbFile correctionsFile = getMbFile(correctionsFileName);
            MbFile chargesFile = getMbFile(chargesFileName);
            if (correctionsFile == null || chargesFile == null) {
                return;
            }

            converter.convertFile(correctionsFile, chargesFile, registryFile.getParent(),
                    registryFile.getName(), tmpDir, mbOrganizationId, messenger);

            /*
            while (!callback.isCompleted()) {
                showIMessages(messenger);
                Thread.sleep(2000);
            }*/

        } catch (NamingException | FileNotFoundException | AbstractException e) {
            log.error("Can not transform", e);
        } finally {
            ec.close();
        }

    }

    public static Long getMbOrganizationId(Properties transformerProperties) {
        String mbOrganizationId = transformerProperties.getProperty(MB_ORGANIZATION_ID);
        if (mbOrganizationId == null) {
            log.error("Config did not content {}", MB_ORGANIZATION_ID);
            return null;
        }
        try {
            return Long.parseLong(mbOrganizationId);
        } catch (NumberFormatException e) {
            log.error("{} have failed number format", mbOrganizationId);
        }
        return null;
    }

    private static MbFile getMbFile(String pathname) throws FileNotFoundException {
        File file = new File(pathname);
        if (!file.isFile()) {
            log.error("{} is not file", pathname);
            return null;
        }
        return new MbFile(file.getParent(), file.getName());
    }

    public static boolean isDirectory(String tmpDirPath) {
        File tmpDir = new File(tmpDirPath);
        return tmpDir.exists() && tmpDir.isDirectory();
    }

    /*
    private static void showIMessages(IMessenger imessenger) {
        if (imessenger.countIMessages() > 0) {
            IMessenger.IMessage importMessage;

            while ((importMessage = imessenger.getNextIMessage()) != null) {
                switch (importMessage.getType()) {
                    case ERROR:
                        log.error(importMessage.getLocalizedString(locale));
                        break;
                    case INFO:
                        log.info(importMessage.getLocalizedString(locale));
                        break;
                }
            }
        }
    }*/

}
