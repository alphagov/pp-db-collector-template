package uk.gov.gds.performance.collector;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class CaptureSystemStreamFixture implements TestRule {
    protected PrintStream originalStream;
    public final ByteArrayOutputStream capturedStream = new ByteArrayOutputStream();

    protected abstract void captureOriginalStream() throws Exception;

    protected abstract void restoreOriginalStream() throws Exception;

    public String getContentsAsString() {
        return new String(capturedStream.toByteArray());
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                capturedStream.reset();
                captureOriginalStream();
                try {
                    base.evaluate();
                } finally {
                    restoreOriginalStream();
                }
            }
        };
    }

    public static class SystemOut extends CaptureSystemStreamFixture {
        @Override
        protected void captureOriginalStream() throws Exception {
            originalStream = System.out;
            System.setOut(new PrintStream(capturedStream, true));
        }

        @Override
        protected void restoreOriginalStream() throws Exception {
            System.setOut(originalStream);
        }
    }

    public static class SystemErr extends CaptureSystemStreamFixture {
        @Override
        protected void captureOriginalStream() throws Exception {
            originalStream = System.err;
            System.setErr(new PrintStream(capturedStream, true));
        }

        @Override
        protected void restoreOriginalStream() throws Exception {
            System.setErr(originalStream);
        }
    }
}
