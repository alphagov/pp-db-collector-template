package uk.gov.gds.performance.collector;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.*;
import org.kohsuke.args4j.CmdLineException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.UUID;

import static org.junit.Assert.*;

public class CommandLineArgumentsTest {

    public static final Instant RIGHT_NOW_ACCORDING_TO_THIS_TEST = new Instant("1993-02-02T06:00:00Z");

    //region hard code joda-time system clock for this test
    @BeforeClass
    public static void setJodaTimeToFixedTime() {
        DateTimeUtils.setCurrentMillisFixed(RIGHT_NOW_ACCORDING_TO_THIS_TEST.getMillis());
    }

    @AfterClass
    public static void resetJodaTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }
    //endregion

    //region capture system out
    private PrintStream originalSystemOut;
    ByteArrayOutputStream capturedSystemOut = new ByteArrayOutputStream();

    @Before
    public void setFakeSystemOut() throws Exception {
        originalSystemOut = System.out;
        System.setOut(new PrintStream(capturedSystemOut, true));
    }

    @After
    public void restoreOriginalSystemOut() throws Exception {
        System.setOut(originalSystemOut);
    }
    //endregion

    //region capture system error
    private PrintStream originalSystemErr;
    ByteArrayOutputStream capturedSystemErr = new ByteArrayOutputStream();

    @Before
    public void setFakeSystemErr() throws Exception {
        originalSystemErr = System.err;
        System.setErr(new PrintStream(capturedSystemErr, true));
    }

    @After
    public void restoreOriginalSystemErr() throws Exception {
        System.setErr(originalSystemErr);
    }
    //endregion

    @Test
    public void getConfigurationFile_shouldReturnASensibleDefault_whenNoArgumentsSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse();

        assertEquals(new File("configuration.properties"), arguments.getConfigurationFile());
    }

    @Test
    public void getConfigurationFile_shouldReturnACustomFile_whenTheFileArgumentIsSpecified() throws Exception {
        File tempFile = buildRandomTempFileThatDoesNotExist();

        CommandLineArguments arguments = CommandLineArguments.parse("--config", tempFile.getAbsolutePath());

        assertEquals(tempFile, arguments.getConfigurationFile());
    }

    @Test
    public void getDateRange_shouldReturnFromThreeDaysAgoToToday_whenNoArgumentsAreSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse();
        LocalDateRange range = arguments.getDateRange();

        assertEquals(new LocalDate(RIGHT_NOW_ACCORDING_TO_THIS_TEST).minusDays(3), range.getStartDate());
        assertEquals(new LocalDate(RIGHT_NOW_ACCORDING_TO_THIS_TEST), range.getEndDate());
    }

    @Test
    public void getDateRange_shouldReturnACustomStartDate_whenTheFromArgumentIsSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse("--from", "1993-01-01");

        LocalDateRange range = arguments.getDateRange();
        assertEquals(new LocalDate("1993-01-01"), range.getStartDate());
    }

    @Test(expected = CmdLineException.class)
    public void parsingCommandLineArguments_shouldBlowUp_givenAnInvalidFromDate() throws Exception {
        String invalidDate = "1993-01-01T00:00:00.000Z";
        CommandLineArguments.parse("--from", invalidDate);
    }

    @Test
    public void getDateRange_shouldReturnACustomEndDate_whenTheToArgumentIsSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse("--to", "1993-12-31");

        LocalDateRange range = arguments.getDateRange();
        assertEquals(new LocalDate("1993-12-31"), range.getEndDate());
    }

    @Test(expected = CmdLineException.class)
    public void parsingCommandLineArguments_shouldBlowUp_givenAnInvalidToDate() throws Exception {
        String invalidDate = "not even remotely a valid date";
        CommandLineArguments.parse("--to", invalidDate);
    }

    @Test
    public void getDateRange_shouldReturnACustomRange_whenTheFromAndToArgumentsAreBothSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse("--from", "2014-01-01", "--to", "2014-12-31");

        LocalDateRange range = arguments.getDateRange();
        assertEquals(new LocalDate("2014-01-01"), range.getStartDate());
        assertEquals(new LocalDate("2014-12-31"), range.getEndDate());
    }

    @Test
    public void isDryRun_shouldReturnFalse_whenTheDryRunFlagIsNotSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse();

        assertFalse(arguments.isDryRun());
    }

    @Test
    public void isDryRun_shouldReturnTrue_whenTheDryRunFlagIsSpecified() throws Exception {
        CommandLineArguments arguments = CommandLineArguments.parse("--dry-run");

        assertTrue(arguments.isDryRun());
    }

    @Test
    public void parse_shouldPrintUsageToSystemErrAndRethrowTheException_givenABadCommandLineOption() throws Exception {
        try {
            CommandLineArguments.parse("--foo");
            fail("expected an exception");
        } catch (CmdLineException e) {
            ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(expectedMessage, true)) {
                out.println(e.getMessage());
                e.getParser().printUsage(out);
            }
            assertEquals(new String(expectedMessage.toByteArray()), new String(capturedSystemErr.toByteArray()));
            assertEquals(0, capturedSystemOut.toByteArray().length);
        }
    }

    @Test
    public void parse_shouldPrintUsageToSystemErrAndThrowACmdLineException_givenAHelpFlag() throws Exception {
        try {
            CommandLineArguments.parse("--help");
            fail("expected an exception");
        } catch (CmdLineException e) {
            ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(expectedMessage, true)) {
                e.getParser().printUsage(out);
            }
            assertEquals(new String(expectedMessage.toByteArray()), new String(capturedSystemErr.toByteArray()));
            assertEquals(0, capturedSystemOut.toByteArray().length);
        }
    }

    @Test
    public void parse_shouldPrintUsageToSystemErrAndThrowACmdLineException_givenAnHFlag() throws Exception {
        try {
            CommandLineArguments.parse("-h");
            fail("expected an exception");
        } catch (CmdLineException e) {
            ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(expectedMessage, true)) {
                e.getParser().printUsage(out);
            }
            assertEquals(new String(expectedMessage.toByteArray()), new String(capturedSystemErr.toByteArray()));
            assertEquals(0, capturedSystemOut.toByteArray().length);
        }
    }

    @Test
    public void parse_shouldPrintUsageToSystemErrAndThrowACmdLineException_givenAQuestionMarkFlag() throws Exception {
        try {
            CommandLineArguments.parse("-h");
            fail("expected an exception");
        } catch (CmdLineException e) {
            ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(expectedMessage, true)) {
                e.getParser().printUsage(out);
            }
            assertEquals(new String(expectedMessage.toByteArray()), new String(capturedSystemErr.toByteArray()));
            assertEquals(0, capturedSystemOut.toByteArray().length);
        }
    }

    private File buildRandomTempFileThatDoesNotExist() {
        return new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + ".tmp");
    }
}