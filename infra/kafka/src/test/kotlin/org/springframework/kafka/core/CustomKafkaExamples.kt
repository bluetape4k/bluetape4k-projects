package org.springframework.kafka.core

import io.bluetape4k.kafka.codec.JacksonKafkaCodec
import io.bluetape4k.kafka.codec.StringKafkaCodec
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.mq.KafkaServer
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload

@SpringBootTest
class CustomKafkaExamples {

    data class Greeting(val name: String, val message: String)

    @Configuration
    @EnableKafka
    class TestConfiguration {

        @Bean
        fun producerProperties(): MutableMap<String, Any?> {
            return KafkaServer.Launcher.getProducerProperties().apply {
                this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringKafkaCodec::class.java
                this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaCodec::class.java
            }
        }

        @Bean
        fun producerFactory(): ProducerFactory<String, Greeting> {
            return KafkaServer.Launcher.Spring.getProducerFactory(producerProperties())
        }

        @Bean
        fun consumerProperties(): MutableMap<String, Any?> {
            return KafkaServer.Launcher.getConsumerProperties().apply {
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringKafkaCodec::class.java
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaCodec::class.java
            }
        }

        @Bean
        fun consumerFactory(): ConsumerFactory<String, Greeting> {
            return KafkaServer.Launcher.Spring.getConsumerFactory(consumerProperties())
        }

        @Bean
        fun kafkaTemplate(
            producerFactory: ProducerFactory<String, Greeting>,
            consumerFactory: ConsumerFactory<String, Greeting>,
        ): KafkaTemplate<String, Greeting> {
            return KafkaTemplate(producerFactory, true).apply {
                defaultTopic = CUSTOM_TOPIC_NAME
                setConsumerFactory(consumerFactory)
            }
        }

        @Bean
        fun kafkaListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, Greeting>,
        ): ConcurrentKafkaListenerContainerFactory<String, Greeting> {
            return KafkaServer.Launcher.Spring.getConcurrentKafkaListenerContainerFactory(consumerFactory)
        }
    }

    companion object: KLogging() {
        private const val CUSTOM_TOPIC_NAME = "custom.kafka.string-topic.1"
    }

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Greeting>

    private val receiveCounter = atomic(0)

    @Test
    fun `context loading`() {
        kafkaTemplate.shouldNotBeNull()
    }

    @Test
    fun `send greeting`() = runTest {
        val key = "custom key"
        val message = Greeting("debop", "hello, ")

        val result = kafkaTemplate.send(CUSTOM_TOPIC_NAME, key, message).await()
        log.debug { "produceRecord=${result.producerRecord}" }
        log.debug { "recordMetadata=${result.recordMetadata}" }

        await until { receiveCounter.value >= 2 }
    }

    @KafkaListener(topics = [CUSTOM_TOPIC_NAME], groupId = "custom")
    private fun listen(message: Greeting) {
        log.debug { "Received Message in group foo: $message" }
        receiveCounter.incrementAndGet()
    }

    @KafkaListener(topics = [CUSTOM_TOPIC_NAME], groupId = "custom-with-header")
    private fun listenWithHeaders(
        @Payload message: Greeting,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Headers headers: Map<String, Any?>,
    ) {
        log.debug { "Received message: [$message], partition=$partition, offset=$offset, headers=$headers" }
        receiveCounter.incrementAndGet()
    }
}
