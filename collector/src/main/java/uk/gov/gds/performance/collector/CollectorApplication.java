package uk.gov.gds.performance.collector;

import uk.gov.gds.performance.collector.logging.OpsLogger;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;

public class CollectorApplication implements AutoCloseable {

    private final Connection connection;
    private final Client client;
    private final Collector collector;
    private final CommandLineArguments arguments;
    private final OpsLogger<CollectorLogMessage> logger;

    public CollectorApplication(Connection connection, Client client, Collector collector, CommandLineArguments arguments, OpsLogger<CollectorLogMessage> logger) {

        this.connection = connection;
        this.client = client;
        this.collector = collector;
        this.arguments = arguments;
        this.logger = logger;
    }

    public void execute() throws Exception {
        try {
            if (!arguments.isDryRun()) {
                collector.collect(arguments.getDateRange());
            }
        } catch(ApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.log(CollectorLogMessage.UnknownError, e);
            throw e;
        }
    }

    @Override
    public void close() {
        client.close();
        try {
            connection.close();
        } catch (SQLException ignore) {}
    }
}
