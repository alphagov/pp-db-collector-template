package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.io.File;

@SuppressWarnings("FieldCanBeLocal")
public class CommandLineArguments {
    private CommandLineArguments() {
        //use the factory method
    }

    @Option(name="--config", usage="path to a configuration file")
    private File configurationFile = new File("configuration.properties");

    @Option(name="--dry-run", usage="perform connectivity checks and then exit")
    private boolean dryRun = false;

    @Option(name="--help", usage="print this usage information", aliases = {"-h", "-?"})
    private boolean printUsage = false;

    @Option(name="--from", usage="specify the date to start collecting from")
    private LocalDate from = new LocalDate().minusDays(3);

    @Option(name="--to", usage="specify the date to collecting up to")
    private LocalDate to = new LocalDate();

    public File getConfigurationFile() {
        return configurationFile;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public LocalDateRange getDateRange() {
        return new LocalDateRange(from, to);
    }

    public static CommandLineArguments parse(String... args) throws CmdLineException {
        CommandLineArguments result = new CommandLineArguments();
        CmdLineParser parser = new CmdLineParser(result);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            throw e;
        }

        if (result.printUsage) {
            parser.printUsage(System.err);
            throw new CmdLineException(parser, "terminate application flow");
        }
        return result;
    }

    //region args4j OptionHandler for joda-time LocalDate

    public static class LocalDateOptionHandler extends OneArgumentOptionHandler<LocalDate> {

        private final CmdLineParser parser;

        public LocalDateOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super LocalDate> setter) {
            super(parser, option, setter);
            this.parser = parser;
        }

        @Override
        protected LocalDate parse(String argument) throws CmdLineException {
            try {
                return new LocalDate(argument);
            } catch (IllegalArgumentException e) {
                throw new CmdLineException(parser, e.getMessage(), e);
            }
        }

        @Override
        public String getDefaultMetaVariable() {
            return "YYYY-MM-DD";
        }
    }

    static {
        CmdLineParser.registerHandler(LocalDate.class, LocalDateOptionHandler.class);
    }
    //endregion
}
