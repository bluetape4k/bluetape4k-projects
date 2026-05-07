package io.bluetape4k.kafka.logback.keyprovider

import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.kafka.logback.utils.hashBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class ContextNameKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    companion object: KLogging() {
        private const val LOGGER_CONTEXT_NAME = "logger-context-name"
    }

    override val keyProvider = ContextNameKafkaKeyProvider()

    @Test
    fun `log의 context name으로 kafka key를 생성한다`() {
        loggerContext.name = LOGGER_CONTEXT_NAME
        keyProvider.context = loggerContext

        val key = keyProvider.get(sampleEvent)!!
        log.debug { "key=${key.contentToString()}" }
        key shouldContainSame LOGGER_CONTEXT_NAME.hashBytes()!!
    }
}
