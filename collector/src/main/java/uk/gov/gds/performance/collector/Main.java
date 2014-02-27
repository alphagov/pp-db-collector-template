package uk.gov.gds.performance.collector;

import org.kohsuke.args4j.CmdLineException;
import uk.gov.gds.performance.collector.logging.OpsLogger;
import java.util.logging.LogManager;

public class Main {

    static {
        LogManager.getLogManager().reset();
    }

    public static void main(String... rawArgs) throws Exception {

        try {
            OpsLogger<CollectorLogMessage> logger = new OpsLogger<>(System.out);
            CommandLineArguments args = CommandLineArguments.parse(rawArgs);
            try (CollectorApplication application = new CollectorApplicationFactory(args, logger).build()) {
                application.execute();
            }
        } catch (CmdLineException ignore) {
            System.exit(1);
        }
    }
}