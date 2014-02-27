package uk.gov.gds.performance.collector;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempFileFixture implements TestRule {
    private final List<File> tempFiles = new ArrayList<>();

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                for (File f : tempFiles) {
                    if (f.exists()) {
                        //continue no matter what happens when deleting the file
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
            }
        };
    }

    public File createTempFile(String extension) throws IOException {
        File result = File.createTempFile("tmp", extension);
        register(result);
        return register(result);
    }

    public File createTempFileThatDoesNotExist(String extension) throws IOException {
        File result = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + "." + extension);
        return register(result);
    }

    public File register(File file) {
        file.deleteOnExit();
        tempFiles.add(file);
        return file;
    }
}
