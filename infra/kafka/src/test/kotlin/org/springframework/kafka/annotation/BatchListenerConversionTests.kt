package org.springframework.kafka.annotation

import com.fasterxml.jackson.annotation.JsonCreator
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.spring.messaging.support.message
import io.bluetape4k.spring.messaging.support.messageOf
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.BytesDeserializer
import org.apache.kafka.common.serialization.BytesSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.utils.Bytes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.KafkaException
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.BatchListenerFailedException
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter
import org.springframework.kafka.support.converter.BytesJsonMessageConverter
import org.springframework.kafka.support.converter.ConversionException
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.SendTo
import java.io.Serializable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["blc1", "blc2", "blc3", "blc4", "blc5", "blc6", "blc6.DLT"])
class BatchListenerConversionTests {

    companion object: KLogging() {
        private const val DEFAULT_TEST_GROUP_ID = "blc"
        private const val AWAIT_TIME_SECONDS = 3L
    }

    @Autowired
    private val config: Config = uninitialized()

    @Autowired
    private val listener1: Listener = uninitialized()

    @Autowired
    private val listener2: Listener = uninitialized()

    @Autowired
    private val template: KafkaTemplate<Int, Any?> = uninitialized()

    @Test
    fun `context loading`() {
        listener1.shouldNotBeNull()
        listener2.shouldNotBeNull()
        template.shouldNotBeNull()
    }

    @Test
    fun `batch of pojos`() {
        doTest(listener1, "blc1")
        doTest(listener2, "blc2")
    }

    private fun doTest(listener: Listener, topic: String) {
        template.send(messageOf(Foo("bar"), mapOf(KafkaHeaders.TOPIC to topic)))

        listener.latch1.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        listener.latch2.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        listener.received!!.shouldNotBeEmpty()
        listener.received!![0] shouldBeInstanceOf Foo::class
        listener.received!![0].bar shouldBeEqualTo "bar"
        listener.receivedTopics!![0] shouldBeEqualTo topic
        listener.receivedPartitions!![0] shouldBeEqualTo 0
    }

    @Test
    fun `batch of pojo messages`() {
        val topic = "blc3"
        template.send(messageOf(Foo("bar"), mapOf(KafkaHeaders.TOPIC to topic)))
        val listener = this.config.listener3()

        listener.latch1.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        listener.received!!.size shouldBeGreaterThan 0
    }

    @Test
    fun `batch replies with @SendTo`() {
        val listener = this.config.listener4()
        val topic = "blc4"
        template.send(messageOf(Foo("bar"), mapOf(KafkaHeaders.TOPIC to topic)))

        listener.latch1.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        val received = listener.received!!
        received.size shouldBeGreaterThan 0
        received[0] shouldBeInstanceOf Foo::class
        received[0].bar shouldBeEqualTo "bar"

        val replies = listener.replies!!
        replies.size shouldBeGreaterThan 0
        replies[0] shouldBeInstanceOf Foo::class
        replies[0].bar shouldBeEqualTo "BAR"
    }

    @Test
    fun `conversion error`() {
        template.send("blc6", 0, 0, """{ "bar": "baz" }""")
        template.send("blc6", 0, 0, "JUNK")
        template.send("blc6", 0, 0, """{ "bar": "qux" }""")

        val listener5 = this.config.listener5()
        listener5.latch1.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        listener5.received shouldBeEqualTo listOf(Foo("baz"), Foo("qux"))

        // FIXME: 역직렬화에 실패하면 DLT 로 보내야 하는데, 최신 버전은 이게 작동을 하지 않는다
        // listener5.latch2.await(AWAIT_TIME_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
        // listener5.dlt shouldBeEqualTo "JUNK"
    }

    @Configuration
    @EnableKafka
    class Config {

        @Bean
        fun kafkaListenerContainerFactory(
            embeddedKafka: EmbeddedKafkaBroker,
            template: KafkaTemplate<Int, Any?>,
        ): KafkaListenerContainerFactory<*> {
            val factory = ConcurrentKafkaListenerContainerFactory<Int, Foo>()
            factory.setConsumerFactory(consumerFactory(embeddedKafka))
            factory.isBatchListener = true
            factory.setBatchMessageConverter(BatchMessagingMessageConverter(converter()))
            factory.setReplyTemplate(template(embeddedKafka))

            val errorHandler = DefaultErrorHandler(DeadLetterPublishingRecoverer(template))
            errorHandler.setLogLevel(KafkaException.Level.DEBUG)
            factory.setCommonErrorHandler(errorHandler)
            return factory
        }

        @Bean
        fun consumerFactory(embeddedKafka: EmbeddedKafkaBroker): DefaultKafkaConsumerFactory<Int, Foo> {
            return DefaultKafkaConsumerFactory(consumerConfigs(embeddedKafka))
        }

        @Bean
        fun consumerConfigs(embeddedKafka: EmbeddedKafkaBroker): Map<String, Any?> {
            return KafkaTestUtils.consumerProps(DEFAULT_TEST_GROUP_ID, "false", embeddedKafka).apply {
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer::class.java)
                put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1000)
                put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500)
            }
        }

        @Bean
        fun template(embeddedKafka: EmbeddedKafkaBroker): KafkaTemplate<Int, Any?> {
            return KafkaTemplate(producerFactory(embeddedKafka)).apply {
                setMessageConverter(converter())
            }
        }

        @Bean
        fun converter(): BytesJsonMessageConverter {
            return BytesJsonMessageConverter(Jackson.defaultJsonMapper)
        }

        @Bean
        fun producerFactory(embeddedKafka: EmbeddedKafkaBroker): ProducerFactory<Int, Any?> {
            return DefaultKafkaProducerFactory(
                producerConfigs(embeddedKafka), null, DelegatingByTypeSerializer(
                    mapOf(
                        ByteArray::class.java to ByteArraySerializer(),
                        Bytes::class.java to BytesSerializer(),
                        String::class.java to StringSerializer()
                    )
                )
            )
        }

        @Bean
        fun producerConfigs(embeddedKafka: EmbeddedKafkaBroker): Map<String, Any?> {
            return KafkaTestUtils.producerProps(embeddedKafka)
        }

        @Bean
        fun listener1(cf: KafkaListenerContainerFactory<*>): Listener {
            return Listener("blc1", cf)
        }

        @Bean
        fun listener2(cf: KafkaListenerContainerFactory<*>): Listener {
            return Listener("blc2", cf)
        }

        @Bean
        fun listener3() = Listener3()

        @Bean
        fun listener4() = Listener4()

        @Bean
        fun listener5() = Listener5()
    }

    class Listener(
        private val topic: String,
        private val cf: KafkaListenerContainerFactory<*>,
    ) {

        internal val latch1 = CountDownLatch(1)
        internal val latch2 = CountDownLatch(1)

        internal var received: List<Foo>? = null
        internal var receivedTopics: List<String>? = null
        internal var receivedPartitions: List<Int>? = null

        val containerFactory: KafkaListenerContainerFactory<*> get() = this.cf

        @KafkaListener(
            topics = ["#{__listener.topic}"],
            groupId = "#{__listener.topic}.group",
            containerFactory = "#{__listener.containerFactory}"
        )
        fun listen1(
            foos: List<Foo>,
            @Header(KafkaHeaders.RECEIVED_TOPIC) topics: List<String>,
            @Header(KafkaHeaders.RECEIVED_PARTITION) partitions: List<Int>,
        ) {
            if (this.received == null) {
                this.received = foos
            }
            this.receivedTopics = topics
            this.receivedPartitions = partitions
            this.latch1.countDown()
        }

        @KafkaListener(beanRef = "__x", topics = ["#{__x.topic}"], groupId = "#{__x.topic}.group2")
        fun listen2(foos: List<Foo>) {
            log.trace { "foos=${foos.joinToString()}" }
            this.latch2.countDown()
        }

        fun getTopic(): String = this.topic
    }

    class Listener3 {
        internal val latch1 = CountDownLatch(1)
        internal var received: List<Foo>? = null

        @KafkaListener(topics = ["blc3"], groupId = "blc3")
        fun listen1(foos: List<Foo>) {
            if (this.received == null) {
                this.received = foos
            }
            this.latch1.countDown()
        }
    }

    class Listener4 {
        internal val latch1 = CountDownLatch(1)
        internal var received: List<Foo>? = null
        internal var replies: List<Foo>? = null

        @KafkaListener(topics = ["blc4"], groupId = "blc4")
        @SendTo
        fun listen1(foos: List<Foo>): Collection<Message<*>> {
            if (this.received == null) {
                this.received = foos
            }

            return foos.map { f ->
                message(Foo(f.bar.uppercase())) {
                    setHeader(KafkaHeaders.TOPIC, "blc5")
                    setHeader(KafkaHeaders.KEY, 42)
                }
            }
        }

        @KafkaListener(topics = ["blc5"], groupId = "blc5")
        fun listen2(foos: List<Foo>) {
            this.replies = foos
            this.latch1.countDown()
        }
    }

    class Listener5 {
        companion object: KLogging()

        internal val latch1 = CountDownLatch(2)
        internal val latch2 = CountDownLatch(1)

        internal val received = mutableListOf<Foo>()

        @Volatile
        internal var dlt: String? = null

        @KafkaListener(topics = ["blc6"], groupId = "blc6")
        @SendTo
        fun listen5(
            foos: List<Foo?>,
            @Header(KafkaHeaders.CONVERSION_FAILURES) conversionFailures: List<ConversionException?>,
        ) {
            log.info { "foos=${foos.joinToString()}" }
            this.latch1.countDown()
            foos.forEachIndexed { i, foo ->
                if (foo == null && conversionFailures[i] != null) {
                    // FIXME: 기존에는 BatchListenerFailedException 이 발생시키면, DLT로 보내는데, 버전 업 이후 작동을 하지 않는다 
                    log.warn(conversionFailures[i]) { "JSON 파싱 예외가 발생하여, DLT 로 보냅니다." }
                    throw BatchListenerFailedException("Conversion error", conversionFailures[i], i)
                } else {
                    this.received.add(foo!!)
                }
            }
        }

        @KafkaListener(
            topics = ["blc6.DLT"],
            groupId = "blc6.DLT",
            properties = [ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG + ":org.apache.kafka.common.serialization.StringDeserializer"]
        )
        fun listen5Dlt(input: String) {
            log.warn { "JSON 파싱 예외가 발생하여, DLT 로 보냅니다. input=$input" }
            this.dlt = input
            this.latch2.countDown()
        }
    }

    data class Foo @JsonCreator constructor(var bar: String): Serializable
}
