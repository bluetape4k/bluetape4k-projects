package io.bluetape4k.logback.kafka.keyprovider

import io.bluetape4k.logback.kafka.utils.hashBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class ThreadNameKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    companion object: KLogging()

    override val keyProvider = ThreadNameKafkaKeyProvider()

    @Test
    fun `log의 thread name 으로 kafka key 를 생성한다`() {
        val threadName = Thread.currentThread().name

        val key = keyProvider.get(sampleEvent)!!
        log.debug { "key=${key.contentToString()}" }
        key shouldContainSame threadName.hashBytes()!!
    }
}
