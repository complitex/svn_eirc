package ru.flexpay.eirc.mb_transformer.console;

import org.apache.commons.cli.*;
import org.complitex.dictionary.service.exception.AbstractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flexpay.eirc.mb_transformer.entity.MbFile;
import ru.flexpay.eirc.mb_transformer.service.MbCorrectionsFileConverter;
import ru.flexpay.eirc.registry.service.RegistryMessenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/*
import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
*/

/**
 * @author Pavel Sknar
 */
public class Transformer {

    private static final Logger log = LoggerFactory.getLogger(Transformer.class);

    private static final Locale LOCALE = new Locale("ru");

    private static final String PROPERTIES_FILE = "config.properties";

    private static final String MB_ORGANIZATION_ID = "mbOrganizationId";
    private static final String EIRC_ORGANIZATION_ID = "eircOrganizationId";
    private static final String TMP_DIR = "tmpDir";

    public static void main(String... args) throws ParseException {

        Options options = new Options();

        Option help = new Option("h", "help", false, "Print this message");
        options.addOption(help);

        Option optionCorrections = new Option("c", "corrections", true, "Input MB corrections pathname");
        optionCorrections.setRequired(true);
        optionCorrections.setArgName("file");
        options.addOption(optionCorrections);

        Option optionCharges = new Option("n", "charges", true, "Input MB charges pathname");
        optionCharges.setRequired(true);
        optionCharges.setArgName("file");
        options.addOption(optionCharges);

        Option registryOption = new Option("r", "registry", true, "Output EIRC registry pathname");
        registryOption.setRequired(true);
        registryOption.setArgName("file");
        options.addOption(registryOption);

        CommandLineParser commandLineParser = new PosixParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            log.error("{}", ex.toString());
            formatter.printHelp(Transformer.class.getName(), options);
            return;
        }

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
        Long mbOrganizationId = getLong(transformerProperties, MB_ORGANIZATION_ID);
        if (mbOrganizationId == null) {
            return;
        }
        Long eircOrganizationId = getLong(transformerProperties, EIRC_ORGANIZATION_ID);
        if (eircOrganizationId == null) {
            return;
        }
        String tmpDir = transformerProperties.getProperty(TMP_DIR);
        if (!isDirectory(tmpDir)) {
            log.error("Failed properties {}={}", TMP_DIR, tmpDir);
            return;
        }

        /*Map<String, Object> properties = new HashMap<>();
        properties.put(EJBContainer.MODULES, new File("build/jar"));

        EJBContainer ec = EJBContainer.createEJBContainer(properties);
        Context ctx = ec.getContext();
        */
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
            //MbCorrectionsFileConverter converter = (MbCorrectionsFileConverter) ctx.lookup("java:global/eirc-mb-transformer-1.0.0-SNAPSHOT-jar-with-dependencies/MbCorrectionsFileConverter");
            MbCorrectionsFileConverter converter = new MbCorrectionsFileConverter();
            converter.init();

            MbFile correctionsFile = getMbFile(correctionsFileName);
            MbFile chargesFile = getMbFile(chargesFileName);
            if (correctionsFile == null || chargesFile == null) {
                return;
            }

            converter.convertFile(correctionsFile, chargesFile, registryFile.getParent(),
                    registryFile.getName(), tmpDir, mbOrganizationId, eircOrganizationId, messenger);

            /*
            while (!callback.isCompleted()) {
                showIMessages(messenger);
                Thread.sleep(2000);
            }*/

        } catch (FileNotFoundException | AbstractException e) {
            log.error("Can not transform", e);
        } finally {
            //ec.close();
        }

    }

    public static Long getLong(Properties transformerProperties, String propertyName) {
        String value = transformerProperties.getProperty(propertyName);
        if (value == null) {
            log.error("Config did not content {}", propertyName);
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("{} have failed number format", value);
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
