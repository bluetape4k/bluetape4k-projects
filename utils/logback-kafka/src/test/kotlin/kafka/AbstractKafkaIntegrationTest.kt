package io.bluetape4k.logback.kafka

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.utils.ShutdownQueue

abstract class AbstractKafkaIntegrationTest {

    companion object: KLoggingChannel()

    protected val kafka: KafkaServer = KafkaServer(useDefaultPort = true).apply {
        start()
        ShutdownQueue.register(this)
    }

}
