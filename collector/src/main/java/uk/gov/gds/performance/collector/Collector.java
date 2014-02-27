package uk.gov.gds.performance.collector;

import uk.gov.gds.performance.collector.logging.OpsLogger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Collector {
    private final StatsPublisher publisher;
    private final StatsRepository repository;
    private final OpsLogger<CollectorLogMessage> logger;

    public Collector(StatsRepository repository, StatsPublisher publisher, OpsLogger<CollectorLogMessage> logger) {
        this.publisher = publisher;
        this.repository = repository;
        this.logger = logger;
    }

    public void collect(LocalDateRange dateRange) throws SQLException, IOException {
        List<StageResult> results = repository.findStatsBetween(dateRange);
        if (results.size() == 0) {
            logger.log(CollectorLogMessage.NoResultsFoundForDateRange, dateRange.getStartDate(), dateRange.getEndDate());
            return;
        }
        publisher.publish(results);
        logger.log(CollectorLogMessage.Success, results.size());
    }
}