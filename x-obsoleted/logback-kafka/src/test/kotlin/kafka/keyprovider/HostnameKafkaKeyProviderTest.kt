package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.core.CoreConstants
import io.bluetape4k.logback.kafka.utils.hashBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class HostnameKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    companion object: KLogging()

    override val keyProvider = HostnameKafkaKeyProvider()

    @Test
    fun `log의 hostname 기반으로 key를 생성한다`() {
        val hostname = "localhost"
        loggerContext.putProperty(CoreConstants.HOSTNAME_KEY, hostname)
        keyProvider.context = loggerContext

        val key = keyProvider.get(sampleEvent)!!
        log.debug { "key: ${key.contentToString()}" }
        key shouldContainSame hostname.hashBytes()!!
    }
}
