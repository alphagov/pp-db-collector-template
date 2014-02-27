package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;
import org.junit.Test;
import uk.gov.gds.performance.collector.logging.OpsLogger;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static uk.gov.gds.performance.collector.logging.OpsLoggerMockFactory.mockLogger;

public class CollectorTest {

    private final StatsPublisher publisher = mock(StatsPublisher.class);
    private final StatsRepository repository = mock(StatsRepository.class);
    private final OpsLogger<CollectorLogMessage> logger = mockLogger(CollectorLogMessage.class);
    private final Collector collector = new Collector(repository, publisher, logger);

    @Test
    public void collect_shouldCallTheRepository_thenPassTheResultToThePublisher() throws Exception {
        LocalDateRange dateRange = new LocalDateRange(LocalDate.now().minusDays(2), LocalDate.now());
        List<StageResult> expectedResults = buildStageResults(3);
        when(repository.findStatsBetween(dateRange)).thenReturn(expectedResults);

        collector.collect(dateRange);

        verify(repository).findStatsBetween(dateRange);
        verify(publisher).publish(expectedResults);
    }

    @Test
    public void collect_shouldLogASuccessMessageWithTheAppropriateNumberOfRecords_whenNoErrorsOccurAndRecordsAreReturned() throws Exception {
        LocalDateRange dateRange = new LocalDateRange(LocalDate.now().minusDays(2), LocalDate.now());
        List<StageResult> expectedResults = buildStageResults(1);
        when(repository.findStatsBetween(dateRange)).thenReturn(expectedResults);

        collector.collect(dateRange);

        verify(logger).log(CollectorLogMessage.Success, 1);
    }

    @Test
    public void collect_shouldLogAFailureMessage_whenNoRecordsAreFound() throws Exception {
        LocalDateRange dateRange = new LocalDateRange(LocalDate.now().minusDays(2), LocalDate.now());
        when(repository.findStatsBetween(dateRange)).thenReturn(new ArrayList<StageResult>());

        collector.collect(dateRange);

        verify(logger).log(CollectorLogMessage.NoResultsFoundForDateRange, dateRange.getStartDate(), dateRange.getEndDate());
        verifyZeroInteractions(publisher);
    }


    private List<StageResult> buildStageResults(int resultsToBuild) {
        List<StageResult> results = new ArrayList<>();
        for (int i = 0; i < resultsToBuild; i++) {
            StageResult sr = new StageResult(new LocalDate(), Period.day, "blah", 42);
            results.add(sr);
        }
        return results;
    }
}
