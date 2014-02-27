package uk.gov.gds.performance.collector;

import uk.gov.gds.performance.collector.logging.OpsLogger;

import java.io.*;
import java.util.Properties;

public class ConfigurationLoader {

    private final File configurationFile;
    private final OpsLogger<CollectorLogMessage> logger;

    public ConfigurationLoader(File configurationFile, OpsLogger<CollectorLogMessage> logger) {
        this.configurationFile = configurationFile;
        this.logger = logger;
    }

    public Properties loadConfiguration() throws IOException {
        byte[] sampleConfigText = loadResourceContents("/sample.properties");
        try {
            Properties configuration = loadProperties(new FileInputStream(configurationFile));
            Properties sampleConfiguration = loadProperties(new ByteArrayInputStream(sampleConfigText));
            if (!sampleConfiguration.stringPropertyNames().equals(configuration.stringPropertyNames())) {

                System.err.println("Expected Configuration Properties:");
                System.err.println(new String(sampleConfigText));
                logger.log(CollectorLogMessage.InvalidConfigurationFile, configurationFile.getAbsolutePath());
                throw new ApplicationException();
            }
            return configuration;
        } catch (FileNotFoundException e) {
            logger.log(CollectorLogMessage.ConfigurationFileNotFound, configurationFile.getAbsolutePath());
            throw new ApplicationException();
        }
    }

    private Properties loadProperties(InputStream stream) throws IOException {
        Properties p = new Properties();
        try {
            p.load(stream);
            return p;
        } finally {
            stream.close();
        }
    }

    private byte[] loadResourceContents(String resource) throws IOException {
        try (InputStream is = ConfigurationLoader.class.getResourceAsStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int i = is.read(buffer, 0, buffer.length); i >= 0; i = is.read(buffer, 0, buffer.length)) {
                out.write(buffer, 0, i);
            }
            return out.toByteArray();
        }
    }
}