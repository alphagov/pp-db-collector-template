package uk.gov.gds.performance.collector;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.gds.performance.collector.logging.OpsLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.gds.performance.collector.logging.OpsLoggerMockFactory.mockLogger;

public class ConfigurationLoaderTest {

    @Rule
    public CaptureSystemStreamFixture captureSystemOut = new CaptureSystemStreamFixture.SystemOut();

    @Rule
    public CaptureSystemStreamFixture captureSystemErr = new CaptureSystemStreamFixture.SystemErr();

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    private OpsLogger<CollectorLogMessage> mockLogger = mockLogger(CollectorLogMessage.class);

    @Test
    public void loadProperties_shouldReturnAPropertiesInstance_givenAFileWithValidProperties() throws Exception {
        File validConfigurationFile = new File(getClass().getResource("/valid-configuration.properties").getFile());
        ConfigurationLoader configuration = new ConfigurationLoader(validConfigurationFile, mockLogger);

        Properties properties = configuration.loadConfiguration();

        assertEquals("a", properties.getProperty("database.connection.string"));
        assertEquals("b", properties.getProperty("database.username"));
        assertEquals("c", properties.getProperty("database.password"));
        assertEquals("d", properties.getProperty("performance.platform.url"));
        assertEquals("e", properties.getProperty("performance.platform.auth.token"));
        assertEquals("f", properties.getProperty("performance.platform.proxyHost"));
        assertEquals("g", properties.getProperty("performance.platform.proxyUsername"));
        assertEquals("h", properties.getProperty("performance.platform.proxyPassword"));
    }

    @Test
    public void loadProperties_shouldLogAMessagePrintExpectedPropertiesToSystemErrAndThrowAnApplicationException_givenAFileWithAMissingProperty() throws Exception {
        File invalidConfigurationFile = new File(getClass().getResource("/invalid-configuration-missing-one.properties").getFile());
        ConfigurationLoader configuration = new ConfigurationLoader(invalidConfigurationFile, mockLogger);

        try {
            configuration.loadConfiguration();
            fail("expected an exception");
        } catch (ApplicationException e) {
            verify(mockLogger).log(CollectorLogMessage.InvalidConfigurationFile, invalidConfigurationFile.getAbsolutePath());
            verifyNoMoreInteractions(mockLogger);

            for (String line : readResourceContents("/sample.properties")) {
                assertThat(captureSystemErr.getContentsAsString(), CoreMatchers.containsString(line));
            }
        }
    }

    @Test
    public void loadProperties_shouldLogAMessagePrintExpectedPropertiesToSystemErrAndThrowAnApplicationException_givenAFileWithAnExtraProperty() throws Exception {
        File invalidConfigurationFile = new File(getClass().getResource("/invalid-configuration-one-extra.properties").getFile());
        ConfigurationLoader configuration = new ConfigurationLoader(invalidConfigurationFile, mockLogger);

        try {
            configuration.loadConfiguration();
            fail("expected an exception");
        } catch (ApplicationException e) {
            verify(mockLogger).log(CollectorLogMessage.InvalidConfigurationFile, invalidConfigurationFile.getAbsolutePath());
            verifyNoMoreInteractions(mockLogger);

            for (String line : readResourceContents("/sample.properties")) {
                assertThat(captureSystemErr.getContentsAsString(), CoreMatchers.containsString(line));
            }
        }
    }

    @Test
    public void loadProperties_shouldLogAMessageAndThrowAnApplicationException_givenAFileThatDoesNotExist() throws Exception {
        File missingConfigurationFile = tempFiles.createTempFileThatDoesNotExist("properties");
        ConfigurationLoader configuration = new ConfigurationLoader(missingConfigurationFile, mockLogger);

        try {
            configuration.loadConfiguration();
            fail("expected an exception");
        } catch (ApplicationException e) {
            verify(mockLogger).log(CollectorLogMessage.ConfigurationFileNotFound, missingConfigurationFile.getAbsolutePath());
            verifyNoMoreInteractions(mockLogger);
        }
    }

    private List<String> readResourceContents(String resource) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader read = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resource)))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                result.add(line);
            }
        }
        return result;
    }
}
