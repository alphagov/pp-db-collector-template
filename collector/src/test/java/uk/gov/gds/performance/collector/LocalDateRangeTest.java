package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mutabilitydetector.unittesting.AllowedReason.provided;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

public class LocalDateRangeTest {
    @Test
    public void class_shouldBeEffectivelyImmutable() throws Exception {
        //LocalDate is effectively immutable, according to joda time spec
        assertInstancesOf(LocalDateRange.class, areImmutable(), provided(LocalDate.class).isAlsoImmutable());
    }

    @Test
    public void constructor_shouldThrowAnIllegalArgumentException_givenANullStartDate() throws Exception {
        LocalDate endDate = new LocalDate("2014-01-01");

        try {
            new LocalDateRange(null, endDate);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("startDate cannot be null", e.getMessage());
        }
    }

    @Test
    public void constructor_shouldThrowAnIllegalArgumentException_givenANullEndDate() throws Exception {
        LocalDate startDate = new LocalDate("2014-01-01");

        try {
            new LocalDateRange(startDate, null);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("endDate cannot be null", e.getMessage());
        }
    }

    @Test
    public void constructor_shouldSwapDates_whenTheStartDateIsAfterTheEndDate() throws Exception {
        LocalDate startDate = new LocalDate("2014-12-31");
        LocalDate endDate = new LocalDate("2014-01-01");

        LocalDateRange range = new LocalDateRange(startDate, endDate);

        assertEquals(endDate, range.getStartDate());
        assertEquals(startDate, range.getEndDate());
    }

    @Test
    public void constructor_shouldSetStartAndEndDateProperties() throws Exception {
        LocalDate startDate = new LocalDate("2014-01-01");
        LocalDate endDate = new LocalDate("2014-12-31");

        LocalDateRange range = new LocalDateRange(startDate, endDate);

        assertEquals(startDate, range.getStartDate());
        assertEquals(endDate, range.getEndDate());
    }
}
