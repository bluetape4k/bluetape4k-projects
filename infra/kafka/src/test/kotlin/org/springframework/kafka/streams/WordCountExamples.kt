package org.springframework.kafka.streams

import io.bluetape4k.kafka.streams.kstream.consumedOf
import io.bluetape4k.kafka.streams.kstream.groupedOf
import io.bluetape4k.kafka.streams.kstream.materializedOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.kstream.ValueMapper
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.streams.WordCountExamples.Companion.INPUT_TOPIC
import org.springframework.kafka.streams.WordCountExamples.Companion.OUTPUT_TOPIC
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = [INPUT_TOPIC, OUTPUT_TOPIC],
    brokerProperties = [
        "auto.create.topics.enable=\${topics.authCreate:false}",
        "delete.topic.enable=\${topic.delete:true}"
    ],
    brokerPropertiesLocation = "classpath:/\${broker.filename:broker}.properties"
)
class WordCountExamples {

    companion object: KLogging() {
        internal const val INPUT_TOPIC = "word-input-topic"
        internal const val OUTPUT_TOPIC = "word-output-topic"
    }

    @Autowired
    private val embeddedKafka: EmbeddedKafkaBroker = uninitialized()

    @Autowired
    private val template: KafkaTemplate<String, String> = uninitialized()

    @Autowired
    private val wordCountProcessor: WordCountProcessor = uninitialized()

    @Autowired
    private val streamsBuilder: StreamsBuilder = uninitialized()

    @Test
    fun `context loading`() {
        template.shouldNotBeNull()
    }

    @Test
    fun `word counting`() {
        val topology = streamsBuilder.build()

        TopologyTestDriver(topology, Properties()).use {
            val inputTopic = it.createInputTopic(INPUT_TOPIC, StringSerializer(), StringSerializer())
            val outputTopic = it.createOutputTopic(OUTPUT_TOPIC, StringDeserializer(), LongDeserializer())

            inputTopic.pipeInput("key", "hello world")
            inputTopic.pipeInput("key2", "hello")

            outputTopic.readKeyValuesToList() shouldContainSame listOf(
                KeyValue.pair("hello", 1L),
                KeyValue.pair("world", 1L),
                KeyValue.pair("hello", 2L)
            )
        }
    }

    @Configuration
    @EnableKafka
    @EnableKafkaStreams
    class Config {

        @Value("\${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
        val brokerAddresses: String = uninitialized()

        @Bean
        fun producerConfig(): Map<String, Any?> {
            return KafkaTestUtils.producerProps(brokerAddresses)
        }

        @Bean
        fun producerFactory(): ProducerFactory<String?, String?> {
            return DefaultKafkaProducerFactory(producerConfig())
        }

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String?, String?> {
            return KafkaTemplate(producerFactory())
        }

        @Value("\${spring.kafka.streams.state.dir:streams-state}")
        private var stateStoreLocation: String? = null

        @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
        fun kStreamsConfigs(): KafkaStreamsConfiguration? {
            val props: MutableMap<String, Any?> = HashMap()
            props[StreamsConfig.APPLICATION_ID_CONFIG] = "wordcount-app"
            props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokerAddresses
            props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Integer().javaClass.name
            props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
            props[StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG] = WallclockTimestampExtractor::class.java.name
            props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "100"

            // configure the state location to allow tests to use clean state for every run
            props[StreamsConfig.STATE_DIR_CONFIG] = stateStoreLocation
            return KafkaStreamsConfiguration(props)
        }

        @Bean
        fun wordCountProcessor(streamsBuilder: StreamsBuilder): WordCountProcessor {
            return WordCountProcessor().apply {
                buildPipeline(streamsBuilder)
            }
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
    }

    class WordCountProcessor {
        companion object: KLogging() {
            private val SERDE_STRING = Serdes.String()
        }

        fun buildPipeline(streamsBuilder: StreamsBuilder) {
            val messageStream: KStream<String, String> = streamsBuilder.stream(
                INPUT_TOPIC,
                consumedOf(SERDE_STRING, SERDE_STRING)
            )

            val wordCounts: KTable<String, Long> = messageStream
                .mapValues(ValueMapper { it.lowercase() })
                .flatMapValues { value -> value.split("\\W+".toRegex()) }
                .groupBy(
                    { _, word -> word },
                    groupedOf(SERDE_STRING, SERDE_STRING)
                )
                .count(materializedOf("counts"))

            wordCounts.toStream().apply { print(Printed.toSysOut()) }
                .to(OUTPUT_TOPIC)
        }
    }
}
