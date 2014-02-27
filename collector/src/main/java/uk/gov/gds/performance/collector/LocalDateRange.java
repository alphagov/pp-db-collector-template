package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;

public final class LocalDateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public LocalDateRange(LocalDate startDate, LocalDate endDate) {
        validateNotNull("startDate", startDate);
        validateNotNull("endDate", endDate);

        if (startDate.isAfter(endDate)) {
            LocalDate swap = endDate;
            endDate = startDate;
            startDate = swap;
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

    private static void validateNotNull(String parameterName, LocalDate parameterValue) {
        if (parameterValue == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
