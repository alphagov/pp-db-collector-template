package uk.gov.gds.performance.collector;

import com.pyruby.stubserver.Header;
import com.pyruby.stubserver.StubMethod;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.gds.performance.collector.logging.OpsLogger;
import uk.gov.gds.performance.collector.logging.OpsLoggerMockFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class CollectorApplicationFactoryTest {

    private final OpsLogger<CollectorLogMessage> mockLogger = OpsLoggerMockFactory.mockLogger(CollectorLogMessage.class);
    private final CommandLineArguments mockArguments = mock(CommandLineArguments.class);
    private final CollectorApplicationFactory collectorApplicationFactory = new CollectorApplicationFactory(mockArguments, mockLogger);

    @Rule
    public StubServerFixture stubServer = new StubServerFixture();

    @Rule
    public CaptureSystemStreamFixture captureSystemOut = new CaptureSystemStreamFixture.SystemOut();

    @Rule
    public CaptureSystemStreamFixture captureSystemErr = new CaptureSystemStreamFixture.SystemErr();

    @Rule
    public DefaultUncaughtExceptionFixture captureDefaultExceptions = new DefaultUncaughtExceptionFixture();

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Test
    public void build_shouldCreateAProperlyWiredCollector() throws Exception {
        File configFile = saveConfigFile(createValidApplicationProperties());
        doReturn(configFile).when(mockArguments).getConfigurationFile();
        stubServer.getServer().expect(StubMethod.post("/foo")).thenReturn(200, "text/plain", "awake");

        CollectorApplication application = collectorApplicationFactory.build();

        assertNotNull(application);
        stubServer.getServer().verify();
    }

    @Test
    public void build_shouldCreateAProperlyWiredCollector_givenAConfigurationThatIncludesAProxyServer() throws Exception {
        Properties properties = createValidApplicationProperties();
        properties.setProperty("performance.platform.url", "http://localhost:" + (stubServer.getServer().getLocalPort() + 100) + "/foo");
        properties.setProperty("performance.platform.proxyHost", "http://localhost:" + stubServer.getServer().getLocalPort());
        properties.setProperty("performance.platform.proxyUsername", "foo");
        properties.setProperty("performance.platform.proxyPassword", "bar");
        File configFile = saveConfigFile(properties);
        doReturn(configFile).when(mockArguments).getConfigurationFile();

        String expectedProxyAuthorizationHeader = "Basic Zm9vOmJhcg=="; //foo:bar base64 encoded

        stubServer.getServer().expect(StubMethod.post("/foo")).thenReturn(407, "text/plain", "auth needed", new ArrayList<Header>() {{ add(Header.header("Proxy-Authenticate", "Basic realm=\"StubServer\"")); }});
        stubServer.getServer().expect(StubMethod.post("/foo").ifHeader("Proxy-Authorization", expectedProxyAuthorizationHeader)).thenReturn(200, "text/plain", "awake via proxy");

        CollectorApplication application = collectorApplicationFactory.build();

        assertNotNull(application);
        stubServer.getServer().verify();
    }

    @Test
    public void build_shouldLogAMessage_whenAllConnectivityChecksPass() throws Exception {
        File configFile = saveConfigFile(createValidApplicationProperties());
        doReturn(configFile).when(mockArguments).getConfigurationFile();
        stubServer.getServer().expect(StubMethod.post("/foo")).thenReturn(200, "text/plain", "awake");

        collectorApplicationFactory.build();

        verify(mockLogger).log(CollectorLogMessage.AllConnectivityChecksPassed);
    }

    @Test
    public void build_shouldLogAMessageAndThrowAnApplicationException_givenDatabaseConnectionIsInvalid() throws Exception {
        Properties p = createValidApplicationProperties();
        p.setProperty("database.connection.string", "jdbc:invalid:mem:foo");
        File configFile = saveConfigFile(p);
        doReturn(configFile).when(mockArguments).getConfigurationFile();

        try {
            collectorApplicationFactory.build();
            fail("expected an exception");
        } catch(ApplicationException e) {
            verify(mockLogger).log(eq(CollectorLogMessage.CouldNotConnectToDatabase), isA(SQLException.class));
            verifyNoMoreInteractions(mockLogger);
        }
    }

    @Test
    public void build_shouldLogAMessageAndThrowAnApplicationException_whenAConnectionToThePerformancePlatformRestApiCannotBeMade() throws Exception {
        Properties p = createValidApplicationProperties();
        String expectedPerformancePlatformUrl = "http://localhost:" + (stubServer.getServer().getLocalPort() + 1) + "/";
        p.setProperty("performance.platform.url", expectedPerformancePlatformUrl);
        File configFile = saveConfigFile(p);
        doReturn(configFile).when(mockArguments).getConfigurationFile();

        try {
            collectorApplicationFactory.build();
            fail("expected an exception");
        } catch(ApplicationException e) {
            verify(mockLogger).log(CollectorLogMessage.CouldNotConnectToPerformancePlatform, expectedPerformancePlatformUrl);
            verifyNoMoreInteractions(mockLogger);
        }
    }

    @Test
    public void build_shouldLogAMessageAndThrowAnApplicationException_whenTheTestQueryToThePerformancePlatformDoesNotWork() throws Exception {
        Properties p = createValidApplicationProperties();
        String expectedPerformancePlatformUrl = p.getProperty("performance.platform.url");
        File configFile = saveConfigFile(p);
        int expectedResponseCode = 401;
        stubServer.getServer().expect(StubMethod.post("/foo")).thenReturn(expectedResponseCode, "text/plain", "denied");
        doReturn(configFile).when(mockArguments).getConfigurationFile();

        try {
            collectorApplicationFactory.build();
            fail("expected an exception");
        } catch(ApplicationException e) {
            verify(mockLogger).log(CollectorLogMessage.PerformancePlatformTestQueryFailed, expectedPerformancePlatformUrl, expectedResponseCode);
            verifyNoMoreInteractions(mockLogger);
        }
    }

    @Test
    public void build_shouldSetAGlobalErrorHandlerThatDoesNotWriteToSystemOutOrError() throws Exception {
        File configFile = saveConfigFile(createValidApplicationProperties());
        doReturn(configFile).when(mockArguments).getConfigurationFile();
        collectorApplicationFactory.build();

        Thread.UncaughtExceptionHandler registeredExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        assertNotNull(registeredExceptionHandler);
        assertNotSame(captureDefaultExceptions.getOriginalDefaultUncaughtExceptionHandler(), registeredExceptionHandler);

        //this is really the execute phase
        registeredExceptionHandler.uncaughtException(Thread.currentThread(), new RuntimeException("doesn't matter what exception"));

        assertEquals("", captureSystemOut.getContentsAsString());
        assertEquals("", captureSystemErr.getContentsAsString());
    }

    @Test
    public void build_shouldLogAnErrorMessage_whenSomethingUnexpectedHappens() throws Exception {
        final RuntimeException spanishInquisitionException = new RuntimeException("No one expects the spanish inquisition!");
        doThrow(spanishInquisitionException).when(mockArguments).getConfigurationFile();

        try {
            collectorApplicationFactory.build();
            fail("Expected the spanish inquisition");
        } catch (Exception e) {
            verify(mockLogger).log(CollectorLogMessage.UnknownError, spanishInquisitionException);
            assertSame(spanishInquisitionException, e);
        }
    }

    private Properties createValidApplicationProperties() {
        Properties result = new Properties();
        result.setProperty("database.connection.string", "jdbc:hsqldb:mem:foo");
        result.setProperty("database.username", "sa");
        result.setProperty("database.password", "password");
        result.setProperty("performance.platform.url", "http://localhost:" + stubServer.getServer().getLocalPort() + "/foo");
        result.setProperty("performance.platform.auth.token", "authToken");
        result.setProperty("performance.platform.proxyHost", "");
        result.setProperty("performance.platform.proxyUsername", "");
        result.setProperty("performance.platform.proxyPassword", "");
        return result;
    }

    private File saveConfigFile(Properties p) throws IOException {
        File result = tempFiles.createTempFile("properties");
        p.store(new FileOutputStream(result), null);
        return result;
    }
}
