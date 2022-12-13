package fr.gouv.stopc.robertserver.crypto.test

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.slf4j.LoggerFactory
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

private val LOG_EVENTS = ListAppender<ILoggingEvent>()

private var warnLogsChecked = false

private var errorLogsChecked = false

fun assertThatInfoLogs(): ListAssert<String> = assertThatLogs(Level.INFO)
    .describedAs("Info logs")

fun assertThatWarnLogs(): ListAssert<String> {
    warnLogsChecked = true
    return assertThatLogs(Level.WARN)
        .describedAs("Warn logs")
}

fun assertThatErrorLogs(): ListAssert<String> {
    errorLogsChecked = true
    return assertThatLogs(Level.ERROR)
        .describedAs("Error logs")
}

private fun assertThatLogs(level: Level): ListAssert<String> {
    val infoLogEvents = LOG_EVENTS.list
        .filter { level == it.level }
        .map { it.formattedMessage }
    return Assertions.assertThat(infoLogEvents)
}

class LogbackManager : TestExecutionListener {

    override fun beforeTestMethod(testContext: TestContext) {
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(LOG_EVENTS)
        LOG_EVENTS.list.clear()
        LOG_EVENTS.start()
        warnLogsChecked = false
        errorLogsChecked = false
    }
}
