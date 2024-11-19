package org.springframework.kafka.streams

import io.bluetape4k.kafka.spring.test.utils.getPropertyValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.uninitialized
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.kstream.ValueMapper
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.kstream.internals.TimeWindow
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.apache.kafka.streams.processor.internals.StreamThread
import org.apache.kafka.streams.state.WindowStore
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.expression.Expression
import org.springframework.expression.common.LiteralExpression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.streams.KafkaStreamsTests.Companion.FOOS
import org.springframework.kafka.streams.KafkaStreamsTests.Companion.STREAMING_TOPIC1
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest
@TestPropertySource(properties = ["streaming.topic.two=streamingTopic2"])
@EmbeddedKafka(
    partitions = 1,
    topics = [STREAMING_TOPIC1, "\${streaming.topic.two}", FOOS],
    brokerProperties = [
        "auto.create.topics.enable=\${topics.authCreate:false}",
        "delete.topic.enable=\${topic.delete:true}"
    ],
    brokerPropertiesLocation = "classpath:/\${broker.filename:broker}.properties"
)
class KafkaStreamsTests {

    companion object: KLogging() {
        internal const val STREAMING_TOPIC1 = "streamingTopic1"
        internal const val FOOS = "foos"
    }

    @Autowired
    private val kafkaTemplate: KafkaTemplate<Int, String> = uninitialized()

    @Autowired
    private val resultFuture: CompletableFuture<ConsumerRecord<*, String>?> = uninitialized()

    @Autowired
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean = uninitialized()

    @Autowired
    private val embeddedKafka: EmbeddedKafkaBroker = uninitialized()

    @Value("\${streaming.topic.two}")
    private var streamingTopic2: String = ""

    @Autowired
    private val stateChangeCalled: AtomicBoolean = uninitialized()

    @Test
    fun `test KStreams`() {
//        with(this.embeddedKafka.getKafkaServer(0).config()) {
//            autoCreateTopicsEnable().shouldBeFalse()
//            deleteTopicEnable().shouldBeTrue()
//            brokerId() shouldBeEqualTo 2
//        }
        this.streamsBuilderFactoryBean.stop()

        val stateLatch = CountDownLatch(1)

        this.streamsBuilderFactoryBean.setStateListener { _, _ -> stateLatch.countDown() }
        val exceptionHandler = mockk<StreamsUncaughtExceptionHandler>(relaxUnitFun = true)
        this.streamsBuilderFactoryBean.setStreamsUncaughtExceptionHandler(exceptionHandler)

        this.streamsBuilderFactoryBean.start()

        val payload1 = "foo" + UUID.randomUUID().toString()
        val payload2 = "foo" + UUID.randomUUID().toString()

        this.kafkaTemplate.sendDefault(0, payload1)
        this.kafkaTemplate.sendDefault(0, payload2)
        this.kafkaTemplate.flush()

        val result = resultFuture.get(600, TimeUnit.SECONDS)

        result.shouldNotBeNull()

        result.topic() shouldBeEqualTo streamingTopic2
        result.value() shouldBeEqualTo (payload1.uppercase() + payload2.uppercase())
        result.headers().lastHeader("foo").shouldNotBeNull()
        result.headers().lastHeader("foo").value() shouldBeEqualTo "bar".toUtf8Bytes()
        result.headers().lastHeader("spel").shouldNotBeNull()

        stateLatch.await(10, TimeUnit.SECONDS).shouldBeTrue()

        val kafkaStreams = this.streamsBuilderFactoryBean.kafkaStreams!!
        val threads = kafkaStreams.getPropertyValue<List<StreamThread>>("threads")
        threads[0].uncaughtExceptionHandler.shouldNotBeNull()

        this.stateChangeCalled.get().shouldBeTrue()
    }

    @Configuration
    @EnableKafka
    @EnableKafkaStreams
    class KafkaStreamsConfig {

        @Value("\${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
        val brokerAddresses: String = uninitialized()


        @Value("\${streaming.topic.two}")
        private val streamingTopic2: String? = null

        @Bean
        fun producerFactory(): ProducerFactory<Int?, String?> {
            return DefaultKafkaProducerFactory(producerConfigs()!!)
        }

        @Bean
        fun producerConfigs(): Map<String?, Any?>? {
            return KafkaTestUtils.producerProps(this.brokerAddresses)
        }

        @Bean
        fun template(): KafkaTemplate<Int?, String?>? {
            return KafkaTemplate(producerFactory(), true).apply {
                defaultTopic = STREAMING_TOPIC1
            }
        }

        @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
        fun kStreamsConfigs(): KafkaStreamsConfiguration? {
            val props: MutableMap<String, Any> = HashMap()
            props[StreamsConfig.APPLICATION_ID_CONFIG] = "testStreams"
            props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokerAddresses
            props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Integer().javaClass.name
            props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
            props[StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG] = WallclockTimestampExtractor::class.java.name
            props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "100"
            return KafkaStreamsConfiguration(props)
        }

        @Bean
        fun stateChangeCalled(): AtomicBoolean {
            return AtomicBoolean()
        }

        @Bean
        fun customizer(): StreamsBuilderFactoryBeanConfigurer {
            return StreamsBuilderFactoryBeanConfigurer { fb: StreamsBuilderFactoryBean ->
                fb.setStateListener { newState: KafkaStreams.State?, oldState: KafkaStreams.State? ->
                    log.trace { "newState=$newState, oldState=$oldState" }
                    stateChangeCalled().set(true)
                }
            }
        }

        @Bean
        fun kStream(kStreamBuilder: StreamsBuilder): KStream<Int, String> {
            val stream = kStreamBuilder.stream<Int, String>(STREAMING_TOPIC1)
            val headers: MutableMap<String, Expression> = HashMap()
            headers["foo"] = LiteralExpression("bar")
            val parser = SpelExpressionParser()
            headers["spel"] = parser.parseExpression("context.timestamp() + key + value")

            stream.mapValues<String>(ValueMapper<String, String> { it.uppercase() })
                .mapValues { name: String? -> Foo(name!!) }
                .repartition(Repartitioned.with(Serdes.Integer(), object: JsonSerde<Foo?>() {}))
                .mapValues<String>(ValueMapper<Foo, String> { it.name })
                .groupByKey()
                .windowedBy<TimeWindow>(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(1000)))
                .reduce(
                    { value1: String, value2: String -> value1 + value2 },
                    Materialized.`as`<Int, String, WindowStore<Bytes, ByteArray>>("windowStore")
                )
                .toStream()
                .map { windowedId: Windowed<Int>, value: String ->
                    KeyValue(windowedId.key(), value)
                }
                .filter { _: Int?, s: String -> s.length > 40 }
                .process<Int, String>({ HeaderEnricherProcessor(headers) })
                .to(streamingTopic2)
            stream.print(Printed.toSysOut())
            return stream
        }

        @Bean
        fun consumerConfigs(): Map<String?, Any?>? {
            return KafkaTestUtils.consumerProps(brokerAddresses, "testGroup", "false")
        }

        @Bean
        fun consumerFactory(): ConsumerFactory<Int?, String?> {
            return DefaultKafkaConsumerFactory(consumerConfigs()!!)
        }

        @Bean
        fun kafkaListenerContainerFactory(): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Int?, String?>>? {
            return ConcurrentKafkaListenerContainerFactory<Int, String>().apply {
                this.consumerFactory = consumerFactory()
            }
        }


        @Bean
        fun resultFuture(): CompletableFuture<ConsumerRecord<*, String?>?> {
            return CompletableFuture()
        }


        @KafkaListener(topics = ["\${streaming.topic.two}"])
        fun listener(payload: ConsumerRecord<*, String?>?) {
            resultFuture().complete(payload)
        }
    }

    data class Foo(var name: String)
}
