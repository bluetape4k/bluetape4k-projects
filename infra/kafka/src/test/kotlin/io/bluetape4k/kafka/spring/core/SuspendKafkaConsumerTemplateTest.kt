package io.bluetape4k.kafka.spring.core

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import reactor.kafka.receiver.ReceiverOptions

/**
 * [SuspendKafkaConsumerTemplate]에 대한 테스트 클래스입니다.
 */
class SuspendKafkaConsumerTemplateTest: AbstractKafkaTest() {
    companion object: KLoggingChannel() {
        private val CONSUMER_GROUP = "$TEST_TOPIC_NAME-consumer-template-group"
    }

    @Test
    fun `ConsumerTemplate 생성`() {
        val receiverOptions =
            ReceiverOptions
                .create<String, String>(
                    mapOf(
                        "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                        "group.id" to CONSUMER_GROUP,
                        "key.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                        "value.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                        "auto.offset.reset" to "earliest",
                    ),
                ).subscription(listOf(TEST_TOPIC_NAME))

        val template = SuspendKafkaConsumerTemplate(receiverOptions)
        template.shouldNotBeNull()
    }
}
