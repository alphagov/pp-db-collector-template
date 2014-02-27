#DB collector template

This is an example/template project for a DB collector that inserts statistics into the GDS Performance Platform

##Getting started

This project is built with [Gradle] (http://www.gradle.org/). We use a feature called the "gradle wrapper" that will automatically install
Gradle if you don't have it already. You can generate an IDE template by typing `./gradlew cleanIdea idea` from the command line. This
will generate project files for [Intellij IDEA](http://www.jetbrains.com/idea/). Build the project and run all unit tests by typing
`./gradlew ci`.You can get information on other tasks by typing `./gradlew tasks`.

##Minimum things to change for a specific collector
- Customise `uk.gov.gds.performance.collector.StatsRepository` to match your reporting database structure. Almost everyone will want to
customise the SQL query being executed.
- Add the appropriate JDBC driver to the project.
    - If it is available as a maven dependency, uncomment and modify the "maven style dependency"
line of `dependencies.gradle`.
    - If it needs to be included from the local filesystem (the Oracle driver is an example of this), then uncomment
and modify the "dependency in a file system" line.
    - Uncomment the //runtime jdbcDriver line of `collector/build.gradle` to add the jdbc driver as a runtime dependency
- Change `uk.gov.gds.performance.collector.StageResult` to send more or different fields to the Performance Platform. You will also need to
edit `uk.gov.gds.performance.collector.StageResultToJsonConverter`
- Customise the log message codes in `uk.gov.gds.performance.collector.CollectorLogMessage` — i.e., change the "GDS-" prefix to something else
appropriate. (If you are significantly modifying your collector, you may need more and different log messages. This is fine.).
- Add a JDBC connection string, db username and password, etc to a configuration.properties file (This should be different between test and prod environments).
- Configure your collector with its own Performance Platform bearer token and http endpoint. Contact the performance platform team for more details.