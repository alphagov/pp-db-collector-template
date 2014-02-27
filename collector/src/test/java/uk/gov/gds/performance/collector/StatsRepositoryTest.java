package uk.gov.gds.performance.collector;

import org.hamcrest.*;
import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StatsRepositoryTest {

    private Connection conn;
    private StatsRepository repo;

    @Before
    public void initDatabase() throws IOException, SQLException, SqlToolError {
        Properties props = new Properties();
        props.load(this.getClass().getResourceAsStream("/unittest-db.properties"));
        conn = DriverManager.getConnection(props.getProperty("jdbc.url"), props.getProperty("username"), props.getProperty("password"));
        repo = new StatsRepository(conn);
        String sqlFilePath = getClass().getResource("/test.sql").getFile();
        SqlFile sqlFile = new SqlFile(new File(sqlFilePath));

        sqlFile.setConnection(conn);
        sqlFile.execute();
        sqlFile.closeReader();
        conn.commit();
    }

    @After
    public void tearDown() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("drop table PP_DATA");
        }

        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void findStatsBetween_shouldOnlyPullDataForTheGivenDateRange_givenARangeOfASingleDay() throws Exception {
        LocalDate startDate = new LocalDate("2014-01-22");

        List<StageResult> results = repo.findStatsBetween(new LocalDateRange(startDate, startDate));
        assertEquals(4, results.size());
        for(StageResult result : results) {
            assertEquals(startDate, result.getTimestamp());
        }
    }

    @Test
    public void findStatsBetween_shouldPullDataForTheEntireInclusiveDateRange_givenARangeThatSpansMultipleDays() throws Exception {
        LocalDate startDate = new LocalDate("2014-01-22");
        LocalDate endDate = new LocalDate("2014-01-23");

        List<StageResult> results = repo.findStatsBetween(new LocalDateRange(startDate, endDate));
        assertEquals(8, results.size());
        for(StageResult result : results.subList(0, 4)) {
            assertEquals(startDate, result.getTimestamp());
        }
        for(StageResult result : results.subList(4, 8)) {
            assertEquals(endDate, result.getTimestamp());
        }
    }

    @Test
    public void findStatsBetween_shouldReturnAListOfFullyPopulatedStageResults() throws Exception {
        LocalDate startDate = new LocalDate("2014-01-22");

        List<StageResult> results = repo.findStatsBetween(new LocalDateRange(startDate, startDate));

        StageResult result = results.get(0);
        assertEquals(startDate, result.getTimestamp());
        assertEquals(Period.day, result.getPeriod());
        assertEquals("DIGITAL", result.getChannel());
        assertEquals(52, result.getCount());
    }

    @Test
    public void findStatsBetween_shouldReturnAnEmptyList_givenADateRangeWithNoData() throws Exception {
        LocalDate startDate = new LocalDate("2013-04-01");
        LocalDate endDate = new LocalDate("2013-04-02");

        List<StageResult> results = repo.findStatsBetween(new LocalDateRange(startDate, endDate));

        assertThat(results, isEmpty());
    }

    @Test
    public void findStatsBetween_shouldCloseTheJDBCStatementItCreates() throws Exception {
        Connection mockConnection = mock(Connection.class);
        final Holder<PreparedStatement> spyStatement = new Holder<>();

        //when prepareStatement is called, return a spy
        when(mockConnection.prepareStatement(anyString())).thenAnswer(new Answer<PreparedStatement>() {
            @Override
            public PreparedStatement answer(InvocationOnMock invocation) throws Throwable {
                PreparedStatement rawResult = conn.prepareStatement((String) invocation.getArguments()[0]);
                PreparedStatement spy = spy(rawResult);
                spyStatement.value = spy;
                return spy;
            }
        });

        repo = new StatsRepository(mockConnection);
        LocalDate startDate = new LocalDate("2014-01-23");
        LocalDate endDate = new LocalDate("2014-01-22");

        repo.findStatsBetween(new LocalDateRange(startDate, endDate));

        verify(spyStatement.value).close();
    }

    @Test
    public void findStatsBetween_shouldCloseTheJDBCStatement_whenAnExceptionIsThrown() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        SQLException expectedException = new SQLException("expected!");
        when(mockResultSet.next()).thenThrow(expectedException);

        repo = new StatsRepository(mockConnection);
        LocalDate startDate = new LocalDate("2014-01-23");
        LocalDate endDate = new LocalDate("2014-01-22");

        try {
            repo.findStatsBetween(new LocalDateRange(startDate, endDate));
            fail("Expected an exception to be thrown");
        } catch (SQLException e) {
            assertSame(expectedException, e);
            verify(mockStatement).close();
        }
    }

    private static class Holder<T> {
        T value = null;
    }

    private static Matcher<Collection> isEmpty() {
        return new BaseMatcher<Collection>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof Collection && ((Collection) item).isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("an empty Collection");
            }
        };
    }
}
