package io.bluetape4k.kafka

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.mq.KafkaServer

abstract class AbstractKafkaTest {

    companion object: KLogging() {
        const val TEST_TOPIC_NAME = "$LibraryName.kafka.test-topic.1"
        const val REPEAT_SIZE = 3

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String = Fakers.randomString(1024, 4096)

        protected val kafka: KafkaServer by lazy { KafkaServer.Launcher.kafka }
    }
}
