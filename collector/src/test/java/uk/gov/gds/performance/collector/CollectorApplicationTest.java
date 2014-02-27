package uk.gov.gds.performance.collector;


import org.hamcrest.CoreMatchers;
import org.joda.time.LocalDate;
import org.junit.Test;
import uk.gov.gds.performance.collector.logging.OpsLogger;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static uk.gov.gds.performance.collector.logging.OpsLoggerMockFactory.mockLogger;


public class CollectorApplicationTest {

    private final Connection connection = mock(Connection.class);
    private final Client client = mock(Client.class);
    private final Collector collector = mock(Collector.class);
    private final CommandLineArguments arguments = mock(CommandLineArguments.class);
    private final OpsLogger<CollectorLogMessage> logger = mockLogger(CollectorLogMessage.class);
    private final CollectorApplication application = new CollectorApplication(connection, client, collector, arguments, logger);

    @Test
    public void execute_shouldDoNothing_whenDryRunIsSet() throws Exception {
        when(arguments.isDryRun()).thenReturn(true);

        application.execute();

        verifyZeroInteractions(collector);
        verifyZeroInteractions(connection, client, logger); //true, but interactions with the collector are what we really care about
    }

    @Test
    public void execute_shouldCallTheCollector_whenDryRunIsFalse() throws Exception {
        LocalDateRange expectedDateRange = new LocalDateRange(new LocalDate("2014-01-01"), new LocalDate("2014-12-31"));
        when(arguments.getDateRange()).thenReturn(expectedDateRange);
        when(arguments.isDryRun()).thenReturn(false);

        application.execute();

        verify(collector).collect(expectedDateRange);
    }

    @Test
    public void execute_shouldRethrowWithoutLogging_whenAnApplicationExceptionIsThrown() throws Exception {
        ApplicationException expected = new ApplicationException();
        doThrow(expected).when(collector).collect(any(LocalDateRange.class));

        try {
            application.execute();
            fail("expected an exception");
        } catch (ApplicationException e) {
            assertSame(expected, e);
            verifyZeroInteractions(logger);
        }
    }

    @Test
    public void execute_shouldLogAnErrorMessageAndRethrow_whenAnUnexpectedExceptionIsEncountered() throws Exception {
        RuntimeException spanishInquisitionException = new RuntimeException("something went wrong");
        doThrow(spanishInquisitionException).when(collector).collect(any(LocalDateRange.class));

        try {
            application.execute();
            fail("expected an exception");
        } catch (RuntimeException e) {
            assertSame(spanishInquisitionException, e);
            verify(logger).log(CollectorLogMessage.UnknownError, spanishInquisitionException);
        }
    }

    @Test
    public void class_shouldImplementAutoCloseable() throws Exception {
        assertThat(application, CoreMatchers.isA(AutoCloseable.class));
    }

    @Test
    public void close_shouldCloseTheRestClient() throws Exception {
        application.close();

        verify(client).close();
    }

    @Test
    public void close_shouldCloseTheDatabaseConnection() throws Exception {
        application.close();

        verify(connection).close();
    }

    @Test
    public void close_shouldIgnoreTheException_whenAnExceptionIsThrownClosingTheDatabaseConnection() throws Exception {
        doThrow(new SQLException()).when(connection).close();

        application.close();

        verify(connection).close();
    }
}
