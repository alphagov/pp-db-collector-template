package uk.gov.gds.performance.collector;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class DefaultUncaughtExceptionFixture implements TestRule {
    private Thread.UncaughtExceptionHandler originalDefaultUncaughtExceptionHandler;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                originalDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
                try {
                    base.evaluate();
                } finally {
                    Thread.setDefaultUncaughtExceptionHandler(originalDefaultUncaughtExceptionHandler);
                }
            }
        };
    }

    public Thread.UncaughtExceptionHandler getOriginalDefaultUncaughtExceptionHandler() {
        return originalDefaultUncaughtExceptionHandler;
    }
}
