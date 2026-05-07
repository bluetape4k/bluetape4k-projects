package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach

abstract class AbstractKafkaKeyProviderTest {

    companion object: KLogging()

    protected abstract val keyProvider: KafkaKeyProvider<*>
    protected val loggerContext = LoggerContext()

    protected val sampleEvent = LoggingEvent(
        "fqcn",
        loggerContext.getLogger("logger"),
        Level.TRACE,
        "msg",
        null,
        null
    )

    @BeforeEach
    open fun beforeEach() {
        loggerContext.reset()
    }
}
