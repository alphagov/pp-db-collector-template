package uk.gov.gds.performance.collector;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import uk.gov.gds.performance.collector.logging.OpsLogger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class CollectorApplicationFactory {

    private final CommandLineArguments arguments;
    private final OpsLogger<CollectorLogMessage> logger;

    private Properties configuration;

    public CollectorApplicationFactory(CommandLineArguments arguments, OpsLogger<CollectorLogMessage> logger) {

        this.arguments = arguments;
        this.logger = logger;
    }

    public CollectorApplication build() throws Exception {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new DoNothingExceptionHandler());

            ConfigurationLoader externalConfigurationLoader = new ConfigurationLoader(arguments.getConfigurationFile(), logger);
            configuration = externalConfigurationLoader.loadConfiguration();

            Connection conn = connectToDatabase();

            Client restClient = createRestClient();
            WebTarget target = createAndTestRestWebTarget(restClient);

            logger.log(CollectorLogMessage.AllConnectivityChecksPassed);
            StatsRepository repo = new StatsRepository(conn);
            StatsPublisher publisher = new StatsPublisher(target, new StageResultToJsonConverter());
            Collector collector = new Collector(repo, publisher, logger);
            return new CollectorApplication(conn, restClient, collector, arguments, logger);
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.log(CollectorLogMessage.UnknownError, e);
            throw e;
        }
    }

    private Connection connectToDatabase() {
        try {
            return DriverManager.getConnection(configuration.getProperty("database.connection.string"), configuration.getProperty("database.username"), configuration.getProperty("database.password"));
        } catch (SQLException e) {
            logger.log(CollectorLogMessage.CouldNotConnectToDatabase, e);
            throw new ApplicationException();
        }
    }

    private Client createRestClient() {
        ClientConfig cc = new ClientConfig();
        if (configuration.getProperty("performance.platform.proxyHost", "").length() > 0) {
            cc.property(ClientProperties.PROXY_URI, configuration.getProperty("performance.platform.proxyHost"));
            cc.property(ClientProperties.PROXY_USERNAME, configuration.getProperty("performance.platform.proxyUsername"));
            cc.property(ClientProperties.PROXY_PASSWORD, configuration.getProperty("performance.platform.proxyPassword"));
        }
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
        cc.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newClient(cc);
    }

    private WebTarget createAndTestRestWebTarget(Client restClient) {
        String url = configuration.getProperty("performance.platform.url");
        WebTarget target = restClient.target(url).register(new AddBearerTokenRequestFilter(configuration.getProperty("performance.platform.auth.token")));

        //test the rest server at the other end with a test request
        Entity<JsonArray> json = Entity.json(Json.createArrayBuilder().build());
        try {
            Response result = target.request().post(json);
            if (result.getStatus() != 200) {
                logger.log(CollectorLogMessage.PerformancePlatformTestQueryFailed, url, result.getStatus());
                throw new ApplicationException();
            }
        } catch (ProcessingException e) {
            logger.log(CollectorLogMessage.CouldNotConnectToPerformancePlatform, url);
            throw new ApplicationException();
        }

        return target;
    }

    private static class DoNothingExceptionHandler implements Thread.UncaughtExceptionHandler {
        @SuppressWarnings("NullableProblems")
        @Override
        public void uncaughtException(Thread t, Throwable e) {

        }
    }
}
