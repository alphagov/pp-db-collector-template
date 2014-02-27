package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StageResultTest {

    @Test
    public void get_id_shouldCreateAnIdBasedOnAllExceptTheCountAttribute() throws Exception {
        LocalDate now = LocalDate.now();
        StageResult result = new StageResult(now, Period.day, "DIGITAL", 50);
        assertEquals(result.get_id(), new StageResult(now, Period.day, "DIGITAL", 20).get_id()); //change only the count, and get the same _id

        //change every other field, and observe a different _id
        assertNotEquals(result.get_id(), new StageResult(now.minusDays(1), Period.day, "DIGITAL", 50).get_id());
        assertNotEquals(result.get_id(), new StageResult(now, Period.week, "DIGITAL", 50).get_id());
        assertNotEquals(result.get_id(), new StageResult(now, Period.day, "DIGITA", 50).get_id());
    }
}