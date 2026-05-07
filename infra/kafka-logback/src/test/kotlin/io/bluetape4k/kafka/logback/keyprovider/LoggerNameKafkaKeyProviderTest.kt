package io.bluetape4k.kafka.logback.keyprovider

import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.kafka.logback.utils.hashBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class LoggerNameKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    companion object: KLogging()

    override val keyProvider = LoggerNameKafkaKeyProvider()

    @Test
    fun `log의 logger 기반으로 kafka key를 생성한다`() {
        val key = keyProvider.get(sampleEvent)!!
        log.debug { "key=${key.contentToString()}" }
        key shouldContainSame sampleEvent.loggerName.hashBytes()!!
    }
}
