package uk.gov.gds.performance.collector;

import com.pyruby.stubserver.StubServer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class StubServerFixture implements TestRule {
    private StubServer stubServer;

    @Override
    public Statement apply(final Statement base, Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                org.mortbay.log.Log.setLog(null);
                stubServer = new StubServer();
                stubServer.start();
                try {
                    base.evaluate();
                } finally {
                    stubServer.stop();
                    stubServer = null;
                }
            }
        };
    }

    public StubServer getServer() {
        return stubServer;
    }
}
