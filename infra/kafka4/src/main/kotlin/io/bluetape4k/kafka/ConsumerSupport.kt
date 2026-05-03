package io.bluetape4k.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

/**
 * [KafkaConsumer]를 생성합니다.
 *
 * ```
 * val consumer = consumerOf(
 *    mapOf(
 *      "bootstrap.servers" to "localhost:9092",
 *      "group.id" to "test",
 *      "enable.auto.commit" to "true",
 *      "auto.commit.interval.ms" to "1000",
 *    ),
 *    StringDeserializer(),
 *    StringDeserializer(),
 * )
 * ```
 *
 * @param configs Kafka Consumer 설정
 * @param keyDeserializer Key Deserializer
 * @param valueDeserializer Value Deserializer
 * @return [KafkaConsumer] 인스턴스
 */
fun <K, V> consumerOf(
    configs: Map<String, Any?>,
    keyDeserializer: Deserializer<K>? = null,
    valueDeserializer: Deserializer<V>? = null,
): Consumer<K, V> {
    return KafkaConsumer(configs, keyDeserializer, valueDeserializer)
}

/**
 * [KafkaConsumer]를 생성합니다.
 *
 * ```
 * val consumer = consumerOf(
 *   Properties().apply {
 *    put("bootstrap.servers", "localhost:9092")
 *    put("group.id", "test")
 *    put("enable.auto.commit", "true")
 *    put("auto.commit.interval.ms", "1000")
 *   },
 *   StringDeserializer(),
 *   StringDeserializer(),
 * )
 * ```
 *
 * @param props Kafka Consumer 설정
 * @param keyDeserializer Key Deserializer
 * @param valueDeserializer Value Deserializer
 * @return [KafkaConsumer] 인스턴스
 */
fun <K, V> consumerOf(
    props: Properties,
    keyDeserializer: Deserializer<K>? = null,
    valueDeserializer: Deserializer<V>? = null,
): Consumer<K, V> {
    return KafkaConsumer(props, keyDeserializer, valueDeserializer)
}
