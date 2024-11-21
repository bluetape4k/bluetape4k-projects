package io.bluetape4k.logback.kafka

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.utils.ShutdownQueue

abstract class AbstractKafkaIntegrationTest {

    companion object: KLogging()

    protected val kafka: KafkaServer = KafkaServer(useDefaultPort = true).apply {
        start()
        ShutdownQueue.register(this)
    }

}
