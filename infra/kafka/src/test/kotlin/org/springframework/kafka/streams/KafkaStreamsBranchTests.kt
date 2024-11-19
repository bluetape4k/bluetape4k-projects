package org.springframework.kafka.streams

import io.bluetape4k.kafka.spring.test.utils.consumerProps
import io.bluetape4k.kafka.spring.test.utils.getRecords
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.asBoolean
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldContainSame
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.streams.KafkaStreamsBranchTests.Companion.FALSE_TOPIC
import org.springframework.kafka.streams.KafkaStreamsBranchTests.Companion.TRUE_FALSE_INPUT_TOPIC
import org.springframework.kafka.streams.KafkaStreamsBranchTests.Companion.TRUE_TOPIC
import org.springframework.kafka.support.KafkaStreamBrancher
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.*

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = [TRUE_TOPIC, FALSE_TOPIC, TRUE_FALSE_INPUT_TOPIC]
)
class KafkaStreamsBranchTests {

    companion object: KLogging() {
        internal const val TRUE_TOPIC = "true-output-topic"
        internal const val FALSE_TOPIC = "false-output-topic"
        internal const val TRUE_FALSE_INPUT_TOPIC = "input-topic"
    }

    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, String> = uninitialized()

    @Autowired
    private val embeddedKafka: EmbeddedKafkaBroker = uninitialized()

    @Test
    fun `branching stream`() {
        val falseConsumer = createConsumer()
        this.embeddedKafka.consumeFromEmbeddedTopics(falseConsumer, FALSE_TOPIC)

        val trueConsumer = createConsumer()
        this.embeddedKafka.consumeFromEmbeddedTopics(trueConsumer, TRUE_TOPIC)

        this.kafkaTemplate.sendDefault(true.toString())
        this.kafkaTemplate.sendDefault(true.toString())
        this.kafkaTemplate.sendDefault(false.toString())

        val trueRecords = trueConsumer.getRecords()
        val falseRecords = falseConsumer.getRecords()

        val trueValues = trueRecords.map { it.value() }
        val falseValues = falseRecords.map { it.value() }

        trueValues shouldContainSame listOf("true", "true")
        falseValues shouldContainSame listOf("false")

        falseConsumer.close()
        trueConsumer.close()
    }

    private fun createConsumer(): Consumer<String, String> {
        val consumerProps = embeddedKafka.consumerProps(UUID.randomUUID().toString(), false)
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 10_000)

        val kafkaConsumerFactory =
            DefaultKafkaConsumerFactory(consumerProps, StringDeserializer(), StringDeserializer())

        return kafkaConsumerFactory.createConsumer()
    }

    @Configuration
    @EnableKafkaStreams
    class KafkaStreamsConfig {

        @Value("\${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
        val brokerAddresses: String = uninitialized()

        @Bean
        fun producerConfigs(): Map<String?, Any?>? {
            return KafkaTestUtils.producerProps(this.brokerAddresses)
        }

        @Bean
        fun producerFactory(): ProducerFactory<String?, String?> {
            return DefaultKafkaProducerFactory(producerConfigs()!!)
        }

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String?, String?> {
            return KafkaTemplate(producerFactory(), true).apply {
                defaultTopic = TRUE_FALSE_INPUT_TOPIC
            }
        }

        @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
        fun kStreamsConfigs(): KafkaStreamsConfiguration? {
            val props: MutableMap<String, Any> = HashMap()
            props[StreamsConfig.APPLICATION_ID_CONFIG] = "testStreams"
            props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokerAddresses
            props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
            props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
            props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "100"
            return KafkaStreamsConfiguration(props)
        }

        @Bean
        fun trueFalseStream(kStreamBuilder: StreamsBuilder): KStream<String, String> {
            return KafkaStreamBrancher<String, String>()
                .branch(
                    { _, value -> value.asBoolean() == true },
                    { ks ->
                        ks.to(TRUE_TOPIC, Produced.with(Serdes.String(), Serdes.String()))
                    }
                )
                .branch(
                    { _, value -> value.asBoolean() == false },
                    { ks -> ks.to(FALSE_TOPIC, Produced.with(Serdes.String(), Serdes.String())) }
                )
                .onTopOf(kStreamBuilder.stream(TRUE_FALSE_INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.String())))
        }
    }
}
