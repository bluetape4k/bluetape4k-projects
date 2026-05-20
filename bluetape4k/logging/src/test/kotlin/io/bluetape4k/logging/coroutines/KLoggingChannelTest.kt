package io.bluetape4k.logging.coroutines

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KLoggingChannelTest {

    private val error get() = RuntimeException("Boom!")

    @Test
    fun `logging events are delivered asynchronously`() = runSuspendIO {
        val channel = TestLoggingChannel()
        val appender = channel.attachCapturingAppender()

        try {
            channel.trace { "trace at ${Instant.now()}" }
            channel.debug { "debug at ${Instant.now()}" }
            channel.info { "info at ${Instant.now()}" }
            channel.warn(error) { "warn at ${Instant.now()}" }
            channel.error(error) { "error at ${Instant.now()}" }

            val events = appender.awaitEvents(5)

            events.map { it.level } shouldBeEqualTo listOf(
                ch.qos.logback.classic.Level.TRACE,
                ch.qos.logback.classic.Level.DEBUG,
                ch.qos.logback.classic.Level.INFO,
                ch.qos.logback.classic.Level.WARN,
                ch.qos.logback.classic.Level.ERROR
            )
            events.map { it.formattedMessage.substringBefore(" at ") } shouldBeEqualTo listOf(
                "trace",
                "debug",
                "info",
                "🔥warn",
                "🔥error"
            )
            events.count { it.throwableProxy != null } shouldBeEqualTo 2
        } finally {
            channel.detachCapturingAppender(appender)
            channel.closeAndJoin()
        }
    }

    @Test
    fun `logging in coroutines`() = runSuspendIO {
        val channel = TestLoggingChannel()
        val appender = channel.attachCapturingAppender()

        try {
            val jobs = List(10) {
                launch(Dispatchers.IO) {
                    channel.debug { "Message at $it" }
                }
            }
            jobs.joinAll()

            appender.awaitEvents(10).size shouldBeEqualTo 10
        } finally {
            channel.detachCapturingAppender(appender)
            channel.closeAndJoin()
        }
    }

    @Test
    fun `log message with suspend function`() = runSuspendIO {
        val channel = TestLoggingChannel()
        val appender = channel.attachCapturingAppender()

        try {
            channel.debug { "delay=${runSuspending(100)}" }

            appender.awaitEvents(1).single().formattedMessage shouldBeEqualTo "delay=100"
        } finally {
            channel.detachCapturingAppender(appender)
            channel.closeAndJoin()
        }
    }

    @Test
    fun `closeAndJoin stops collector job`() = runSuspendIO {
        val channel = TestLoggingChannel()

        channel.isClosed.shouldBeFalse()
        channel.collectorActive.shouldBeTrue()

        channel.closeAndJoin()

        channel.isClosed.shouldBeTrue()
        channel.collectorActive.shouldBeFalse()
    }

    @Test
    fun `close is idempotent and drops later events`() = runSuspendIO {
        val channel = TestLoggingChannel()
        val appender = channel.attachCapturingAppender()

        try {
            channel.close()
            channel.close()
            channel.closeAndJoin()

            channel.isClosed.shouldBeTrue()
            channel.collectorActive.shouldBeFalse()

            channel.info { "after-close" }

            appender.events.size shouldBeEqualTo 0
        } finally {
            channel.detachCapturingAppender(appender)
            channel.closeAndJoin()
        }
    }

    private suspend fun runSuspending(delayMillis: Long = 100): Long {
        kotlinx.coroutines.delay(delayMillis.milliseconds)
        return delayMillis
    }

    private class TestLoggingChannel: KLoggingChannel()

    private class CapturingAppender: AppenderBase<ILoggingEvent>() {
        val events = CopyOnWriteArrayList<ILoggingEvent>()

        override fun append(eventObject: ILoggingEvent) {
            events += eventObject
        }
    }

    private fun KLoggingChannel.attachCapturingAppender(): CapturingAppender {
        val logger = log as Logger
        return CapturingAppender().also { appender ->
            appender.context = logger.loggerContext
            appender.start()
            logger.addAppender(appender)
        }
    }

    private fun KLoggingChannel.detachCapturingAppender(appender: CapturingAppender) {
        val logger = log as Logger
        logger.detachAppender(appender)
        appender.stop()
    }

    private suspend fun CapturingAppender.awaitEvents(expectedCount: Int): List<ILoggingEvent> {
        withTimeout(2_000.milliseconds) {
            while (events.size < expectedCount) {
                kotlinx.coroutines.delay(10.milliseconds)
            }
        }
        return events.toList()
    }
}
