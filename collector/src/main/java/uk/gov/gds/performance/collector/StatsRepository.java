package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatsRepository {
    private final Connection conn;

    public StatsRepository(Connection conn) {
        this.conn = conn;
    }

    public List<StageResult> findStatsBetween(LocalDateRange localDateRange) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "select EVENT_TIME, PERIOD, CHANNEL, EVENT_COUNT " +
                "from PP_DATA " +
                "where EVENT_TIME between ? and ?")) {
            stmt.setDate(1, toSqlDate(localDateRange.getStartDate()));
            stmt.setDate(2, toSqlDate(localDateRange.getEndDate()));
            ResultSet resultSet = stmt.executeQuery();

            return convertResultSetToStageResults(resultSet);
        }
    }

    private List<StageResult> convertResultSetToStageResults(ResultSet resultSet) throws SQLException {
        List<StageResult> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add(convertSingleRowToStageResult(resultSet));
        }
        return results;
    }

    private StageResult convertSingleRowToStageResult(ResultSet resultSet) throws SQLException {
        return new StageResult(
                new LocalDate(resultSet.getDate("EVENT_TIME")),
                Period.valueOf(resultSet.getString("PERIOD")),
                resultSet.getString("CHANNEL"),
                resultSet.getInt("EVENT_COUNT")
        );
    }

    private Date toSqlDate(LocalDate startDate) {
        return new Date(startDate.toDate().getTime());
    }
}
