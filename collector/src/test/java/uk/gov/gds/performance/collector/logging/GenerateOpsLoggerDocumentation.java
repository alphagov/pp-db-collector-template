package uk.gov.gds.performance.collector.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateOpsLoggerDocumentation {

    public static void main(String... args) throws Exception {
        String head = args[0];
        String[] tail = Arrays.copyOfRange(args, 1, args.length);
        new GenerateOpsLoggerDocumentation(head, tail).generate();
    }

    private final File outputFile;
    private final List<String> classesToDocument;

    GenerateOpsLoggerDocumentation(String outputFile, String... classesToDocument) {
        this.outputFile = new File(outputFile);
        this.classesToDocument = Arrays.asList(classesToDocument);
    }

    public void generate() throws Exception {

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
            for(String it : classesToDocument) {
                Class<?> clazz = Class.forName(it);
                validate(clazz);
                out.print(clazz.getName());
                out.println(":");
                out.println("Code\t\tMessage");
                out.println("==========\t==========");
                for(Object o : clazz.getEnumConstants()) {
                    LogMessage message = (LogMessage) o;
                    out.printf("%s\t%s\n", message.getMessageCode(), message.getMessagePattern());
                }
                out.println();
            }
        }
    }

    private void validate(Class<?> clazz) {
        if (!clazz.isEnum()) {
            throw new RuntimeException(clazz.getName() + " should be an enum");
        }
        if (!LogMessage.class.isAssignableFrom(clazz)) {
            throw new RuntimeException(clazz.getName() + " should implement LogMessage");
        }
    }
}