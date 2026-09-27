package io.bluetape4k.kafka.logback.keyprovider

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach

abstract class AbstractKafkaKeyProviderTest {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker
    }

    protected abstract val keyProvider: KafkaKeyProvider<*>
    protected val loggerContext = LoggerContext()

    protected val sampleEvent = LoggingEvent(
        "fqcn",
        loggerContext.getLogger("logger"),
        Level.TRACE,
        faker.lorem().paragraph(),
        null,
        null
    )

    @BeforeEach
    open fun beforeEach() {
        loggerContext.reset()
    }
}
