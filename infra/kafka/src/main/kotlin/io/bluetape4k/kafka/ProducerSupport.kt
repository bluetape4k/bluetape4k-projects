package io.bluetape4k.kafka

import io.bluetape4k.support.asDoubleOrNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Serializer
import java.util.*

/**
 * [KafkaProducer]를 생성합니다.
 *
 * ```
 * val producer = producerOf(
 *      mapOf(
 *          "bootstrap.servers" to "localhost:9092",
 *          "acks" to "all",
 *          "retries" to 3,
 *      ),
 *      StringSerializer(),
 *      StringSerializer(),
 * )
 * ```
 *
 * @param configs Kafka Producer 설정
 * @param keySerializer Key Serializer
 * @param valueSerializer Value Serializer
 * @return [KafkaProducer] 인스턴스
 */
fun <K, V> producerOf(
    configs: Map<String, Any?>,
    keySerializer: Serializer<K>? = null,
    valueSerializer: Serializer<V>? = null,
): Producer<K, V> {
    return KafkaProducer(configs, keySerializer, valueSerializer)
}

/**
 * [KafkaProducer]를 생성합니다.
 *
 * ```
 * val producer = producerOf(
 *      Properties().apply {
 *          put("bootstrap.servers", "localhost:9092")
 *          put("acks", "all")
 *          put("retries", 3)
 *      },
 *      StringSerializer(),
 *      StringSerializer(),
 * )
 * ```
 *
 * @param props Kafka Producer 설정
 * @param keySerializer Key Serializer
 * @param valueSerializer Value Serializer
 * @return [KafkaProducer] 인스턴스
 */
fun <K, V> producerOf(
    props: Properties,
    keySerializer: Serializer<K>? = null,
    valueSerializer: Serializer<V>? = null,
): Producer<K, V> {
    return KafkaProducer(props, keySerializer, valueSerializer)
}

/**
 * Producer 의 metrics 측정 값을 조회합니다.
 *
 * ```
 * val sendCount = producer.getMetricValue("record-send-total")
 * ```
 * @param metricName metric name to revrieve
 * @return metric 값
 */
fun <K, V> Producer<K, V>.getMetricValue(metricName: String): Double =
    getMetricValueOrNull(metricName)?.asDoubleOrNull() ?: 0.0

/**
 * Producer 의 metrics 측정 값을 조회합니다.
 *
 * ```
 * val sendCount = producer.getMetricValue("record-send-total")
 * ```
 * @param metricName metric name to revrieve
 * @return metric 값
 */
fun <K, V> Producer<K, V>.getMetricValueOrNull(metricName: String): Any? =
    metrics().entries.find { it.key.name() == metricName }?.value?.metricValue()
