package io.bluetape4k.kafka.spring.test.utils

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

fun EmbeddedKafkaBroker.producerProps(): MutableMap<String, Any?> =
    KafkaTestUtils.producerProps(this)

fun EmbeddedKafkaBroker.consumerProps(group: String, autoCommit: Boolean = false): MutableMap<String, Any?> =
    KafkaTestUtils.consumerProps(this.brokersAsString, group, autoCommit.toString())

fun <K, V> Consumer<K, V>.getEndOffsets(topic: String, vararg partitions: Int): Map<TopicPartition, Long> {
    return KafkaTestUtils.getEndOffsets(this, topic, *partitions.toTypedArray())
}

fun <K, V> Consumer<K, V>.getSingleRecord(
    topic: String,
    timeout: Duration = Duration.ofSeconds(10),
): ConsumerRecord<K, V> =
    KafkaTestUtils.getSingleRecord(this, topic, timeout)

fun <K, V> Consumer<K, V>.getRecords(
    timeout: Duration = Duration.ofSeconds(10),
    minRecords: Int = -1,
): ConsumerRecords<K, V> {
    return KafkaTestUtils.getRecords(this, timeout, minRecords)
}

inline fun <reified T> Any.getPropertyValue(path: String): T =
    KafkaTestUtils.getPropertyValue(this, path, T::class.java)
