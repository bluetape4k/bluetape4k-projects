package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.testcontainers.mq.KafkaServer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.util.*

internal object KafkaProvider {

    const val TEST_TOPIC = "javers.test-topic.1"

    val kafka: KafkaServer = KafkaServer.Launcher.kafka

    val producerProperties: Map<String, Any?> by lazy {
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ProducerConfig.CLIENT_ID_CONFIG to UUID.randomUUID().toString()
        )
    }

    val producerFactory: ProducerFactory<String, String> by lazy {
        DefaultKafkaProducerFactory(
            producerProperties,
            StringSerializer(),
            StringSerializer()
        )
    }

    val consumerProperties: Map<String, Any?> by lazy {
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "tc-" + UUID.randomUUID().toString(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )
    }

    val consumerFactory: ConsumerFactory<String, String> by lazy {
        DefaultKafkaConsumerFactory(
            consumerProperties,
            StringDeserializer(),
            StringDeserializer()
        )
    }

    val kafkaTemplate: KafkaTemplate<String, String> by lazy {
        KafkaTemplate(producerFactory, true).apply {
            defaultTopic = TEST_TOPIC
        }
    }
}
