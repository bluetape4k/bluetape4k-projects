package org.springframework.kafka.core

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import io.bluetape4k.testcontainers.mq.KafkaServer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class SimpleKafkaExamples {

    @Configuration
    @EnableKafka
    class TestConfiguration {
        @Bean
        fun producerFactory(): ProducerFactory<String, String> {
            return KafkaServer.Launcher.Spring.getStringProducerFactory()
        }

        @Bean
        fun consumerFactory(): ConsumerFactory<String, String> {
            return KafkaServer.Launcher.Spring.getStringConsumerFactory()
        }

        @Bean
        fun kafkaTemplate(
            producerFactory: ProducerFactory<String, String>,
            consumerFactory: ConsumerFactory<String, String>,
        ): KafkaTemplate<String, String> {
            return KafkaTemplate(producerFactory, true).apply {
                defaultTopic = SIMPLE_TOPIC_NAME
                setConsumerFactory(consumerFactory)
            }
        }

        @Bean
        fun kafkaListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, String>,
        ): ConcurrentKafkaListenerContainerFactory<String, String> {
            return KafkaServer.Launcher.Spring.getConcurrentKafkaListenerContainerFactory(consumerFactory)
        }

        @Bean
        fun kafkaManualAckListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, String>,
        ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
            return KafkaServer.Launcher.Spring
                .getKafkaManualAckListenerContainerFactory(consumerFactory)
        }
    }

    companion object: KLoggingChannel() {
        private const val SIMPLE_TOPIC_NAME = "simple.kafka.string-topic.1"
    }

    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, String> = uninitialized()

    private val consumed = AtomicInteger(0)

    @BeforeEach
    fun beforeEach() {
        consumed.set(0)
    }

    @Test
    fun `context loading`() {
        kafkaTemplate.shouldNotBeNull()
    }

    @Test
    fun `send string message`() = runTest {
        val key = "simple key"
        val message = "simple message"

        val result = kafkaTemplate.send(SIMPLE_TOPIC_NAME, key, message).await()
        log.debug { "produceRecord=${result.producerRecord}" }
        log.debug { "recordMetadata=${result.recordMetadata}" }
        result.recordMetadata.hasTimestamp().shouldBeTrue()
        result.recordMetadata.hasOffset().shouldBeTrue()

        val result2 = kafkaTemplate.send(SIMPLE_TOPIC_NAME, key, message).await()
        log.debug { "produceRecord=${result2.producerRecord}" }
        log.debug { "recordMetadata=${result2.recordMetadata}" }
        result2.recordMetadata.hasTimestamp().shouldBeTrue()
        result2.recordMetadata.hasOffset().shouldBeTrue()

        // 테스트용 Kafka 라서 partition 은 1만 갖도록 한다 
        result2.recordMetadata.partition() shouldBeEqualTo result.recordMetadata.partition()
        result2.recordMetadata.offset() shouldBeGreaterThan result.recordMetadata.offset()

        await until { consumed.get() >= 2 * 3 }
        log.debug { "all consumer has been consumed." }
    }

    @KafkaListener(
        topics = [SIMPLE_TOPIC_NAME],
        groupId = "simple-group",
        containerFactory = "kafkaManualAckListenerContainerFactory"
    )
    private fun listen(message: String, ack: Acknowledgment) {
        log.debug { "Receive Message in group simple-group: $message" }
        consumed.incrementAndGet()
        runCatching { ack.acknowledge() }
    }

    @KafkaListener(
        topics = [SIMPLE_TOPIC_NAME],
        groupId = "with-header",
        containerFactory = "kafkaManualAckListenerContainerFactory"
    )
    private fun listenWithHeaders(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        ack: Acknowledgment,
    ) {
        log.debug { "Received message: [$message], partition=$partition, offset=$offset" }
        consumed.incrementAndGet()
        runCatching { ack.acknowledge() }
    }

    @KafkaListener(
        topics = [SIMPLE_TOPIC_NAME],
        groupId = "with-record",
        containerFactory = "kafkaManualAckListenerContainerFactory"
    )
    private fun listenWithHeaders(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        log.debug { "Received message: record=$record" }
        consumed.incrementAndGet()
        runCatching { ack.acknowledge() }
    }
}
