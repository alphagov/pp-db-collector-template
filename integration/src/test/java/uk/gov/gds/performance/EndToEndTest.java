package uk.gov.gds.performance;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBAddress;
import com.mongodb.Mongo;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EndToEndTest {

    private Server server;

    @Before
    public void setUp() throws Exception {
        server = startHsql();
        resetBackdropDataSet();
        resetVarnishCache();
        populateDb();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void collectSlcData() throws Exception {
        int result = runCollector();
        assertEquals(0, result);

        Response response = ClientBuilder
                .newClient()
                .target("http://read.backdrop.perfplat.dev/data/slc-test/test")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .buildGet()
                .invoke();
        assertEquals(200, response.getStatus());
        assertEquals(2, response.readEntity(JsonObject.class).getJsonArray("data").size());
    }

    @Test
    public void collectSlcDataBetweenTwoDates() throws Exception {
        int result = runCollector("--from", LocalDate.now().minusDays(10).toString(ISODateTimeFormat.date()), "--to", LocalDate.now().toString(ISODateTimeFormat.date()));
        assertEquals(0, result);

        Response response = ClientBuilder
                .newClient()
                .target("http://read.backdrop.perfplat.dev/data/slc-test/test")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .buildGet()
                .invoke();
        assertEquals(200, response.getStatus());
        assertEquals(30, response.readEntity(JsonObject.class).getJsonArray("data").size());
    }

    @Test
    public void dryRunCollector_shouldNotAddAnyRecordsToBackdrop() throws Exception {
        int result = runCollector("--dry-run");
        assertEquals(0, result);

        Response response = ClientBuilder
                .newClient()
                .target("http://read.backdrop.perfplat.dev/data/slc-test/test")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .buildGet()
                .invoke();
        assertEquals(200, response.getStatus());
        assertEquals(0, response.readEntity(JsonObject.class).getJsonArray("data").size());
    }

    private void resetBackdropDataSet() throws UnknownHostException {
        DB db = Mongo.connect(new DBAddress("localhost", 27037, "backdrop"));
        db.getCollection("student_finance_transactions_by_channel").remove(new BasicDBObject());
    }

    private void resetVarnishCache() {
        ClientConfig cc = new ClientConfig();
        cc.connectorProvider(new ApacheConnectorProvider());
        ClientBuilder
                .newClient(cc)
                .target("http://read.backdrop.perfplat.dev/data/slc-test/test")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .method("PURGE");
    }

    private int runCollector(String... args) throws InterruptedException, IOException {
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-jar", "../collector/build/libs/collector-0.1.jar", "--config=e2econfig.properties"));
        command.addAll(Arrays.asList(args));
        ProcessBuilder b = new ProcessBuilder(command);
        b.redirectInput(ProcessBuilder.Redirect.INHERIT);
        b.redirectError(ProcessBuilder.Redirect.INHERIT);
        return b.start().waitFor();
    }

    private Server startHsql() throws IOException, ServerAcl.AclFormatException {
        HsqlProperties p = new HsqlProperties();
        p.setProperty("server.database.0", "mem:slc");
        p.setProperty("server.dbname.0", "slc");
        p.setProperty("server.port", "9001");

        Server server = new Server();
        server.setProperties(p);
        server.start();
        return server;
    }

    private void populateDb() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:9001/sample", "SA", "");
        Statement statement = conn.createStatement();
        statement.execute("SET DATABASE SQL SYNTAX ORA TRUE");
        try {
            statement.execute("CREATE TABLE PP_DATA (EVENT_TIME timestamp not null, PERIOD varchar2(20) not null, CHANNEL varchar2(250) not null, EVENT_COUNT number(8) not null)");
        } catch (SQLException e) {
            statement.execute("DELETE FROM PP_DATA");
        }
        statement.close();
        LocalDate day = LocalDate.now().minusDays(3);
        for (int i = 0; i < 70; i++) {
            insertRow(conn, day.minusDays(i), "day", "DIGITAL", 5);
            insertRow(conn, day.minusDays(i), "day", "PAPER", 8);
        }
    }

    private void insertRow(Connection conn, LocalDate date, String period, String channel, int count) throws SQLException {
        PreparedStatement prep = conn.prepareStatement("insert into PP_DATA values (?, ?, ?, ?)");
        Date d = new Date(date.toDate().getTime());
        int column = 0;
        prep.setDate(++column, d);
        prep.setString(++column, period);
        prep.setString(++column, channel);
        prep.setInt(++column, count);
        prep.execute();
        conn.commit();
    }
}
