package fr.gouv.stopc.robertserver.ws.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;

public class LogbackManager implements TestExecutionListener {

    private final static ListAppender<ILoggingEvent> LOG_EVENTS = new ListAppender<ILoggingEvent>();

    @Override
    public void beforeTestMethod(TestContext testContext) {
        final var rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(LOG_EVENTS);
        LOG_EVENTS.list.clear();
        LOG_EVENTS.start();
    }

    public static ListAssert<String> assertThatInfoLogs() {
        return assertThatLogs(Level.INFO);
    }

    public static ListAssert<String> assertThatWarnLogs() {
        return assertThatLogs(Level.WARN);
    }

    public static ListAssert<String> assertThatErrorLogs() {
        return assertThatLogs(Level.ERROR);
    }

    private static ListAssert<String> assertThatLogs(final Level level) {
        final var infoLogEvents = LOG_EVENTS.list.stream()
                .filter(event -> level.equals(event.getLevel()))
                .map(ILoggingEvent::getFormattedMessage);
        return Assertions.assertThat(infoLogEvents);
    }
}
