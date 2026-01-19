package org.springframework.kafka.annotation

import io.bluetape4k.jackson.Jackson
import io.bluetape4k.kafka.spring.test.utils.consumerProps
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.converter.BytesJsonMessageConverter
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.FailedDeserializationInfo
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
@EmbeddedKafka(topics = ["blc.2.1"], partitions = 1)
class BatchListenerConversion2Tests {

    companion object: KLoggingChannel() {
        private const val DEFAULT_TEST_GROUP_ID = "blc2"
    }

    @Autowired
    private val config: Config = uninitialized()

    @Autowired
    private val template: KafkaTemplate<Int, Any?> = uninitialized()

    @Test
    fun `conversion error`() {
        val listener = this.config.listener1()
        val topic = "blc.2.1"

        template.send(topic, """{ "bar": "baz" }""")
        template.send(topic, "JUNK")
        template.send(topic, """{ "bar": "qux" }""")

        listener.latch1.await(10, TimeUnit.SECONDS).shouldBeTrue()
        listener.badFoo shouldBeInstanceOf BadFoo::class
        listener.receivedFoos shouldBeEqualTo 2
    }

    @Configuration
    @EnableKafka
    class Config {

        @Autowired
        private val embeddedKafka: EmbeddedKafkaBroker = uninitialized()

        @Bean
        fun kafkaListenerContainerFactory(
            embeddedKafka: EmbeddedKafkaBroker,
            template: KafkaTemplate<Int, Any?>,
        ): KafkaListenerContainerFactory<*> {
            return ConcurrentKafkaListenerContainerFactory<Int, Foo>().apply {
                consumerFactory = consumerFactory()
                isBatchListener = true
                setReplyTemplate(template())
            }
        }

        @Bean
        fun consumerFactory(): DefaultKafkaConsumerFactory<Int, Foo> {
            return DefaultKafkaConsumerFactory(consumerConfigs())
        }

        @Bean
        fun consumerConfigs(): Map<String, Any?> {
            return embeddedKafka.consumerProps(DEFAULT_TEST_GROUP_ID, false).apply {
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer::class.java)
                put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer::class.java)
                put(ErrorHandlingDeserializer.VALUE_FUNCTION, FailedFooProvider::class.java)
                put(JsonDeserializer.VALUE_DEFAULT_TYPE, Foo::class.java.name)
            }
        }

        @Bean
        fun template(): KafkaTemplate<Int, Any?> {
            return KafkaTemplate(producerFactory())
        }

        @Bean
        fun converter(): BytesJsonMessageConverter = BytesJsonMessageConverter(Jackson.defaultJsonMapper)

        @Bean
        fun producerFactory(): ProducerFactory<Int, Any?> {
            return DefaultKafkaProducerFactory(producerConfigs())
        }

        @Bean
        fun producerConfigs(): Map<String, Any?> {
            val props = KafkaTestUtils.producerProps(embeddedKafka)
            props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            return props
        }

        @Bean
        fun listener1() = Listener()

    }

    class Listener {
        internal val latch1 = CountDownLatch(3)

        @Volatile
        internal var badFoo: Foo? = null

        @Volatile
        internal var receivedFoos: Int = 0

        @KafkaListener(id = "deser", topics = ["blc.2.1"])
        fun listen1(foos: List<Foo>) {
            foos.forEach { f ->
                if (f.bar == null) {
                    this.badFoo = f
                } else {
                    receivedFoos++
                }
                latch1.countDown()
            }
        }
    }

    open class Foo(var bar: String? = null) {
        override fun toString(): String = "Foo [bar=$bar]"
    }

    class BadFoo(val failedDeserializationInfo: FailedDeserializationInfo): Foo(null)

    class FailedFooProvider: java.util.function.Function<FailedDeserializationInfo, Foo> {
        override fun apply(failedDeserializationInfo: FailedDeserializationInfo): Foo {
            return BadFoo(failedDeserializationInfo)
        }
    }
}
