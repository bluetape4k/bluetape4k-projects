package io.bluetape4k.kafka

import io.bluetape4k.support.assertNotBlank
import org.apache.kafka.common.TopicPartition

/**
 * `"topic-partition"` 형식 문자열을 [TopicPartition]으로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [topicPartitionOf]에 그대로 위임합니다.
 * - 입력 형식 검증/예외 처리 규칙은 [topicPartitionOf]를 따릅니다.
 * - 수신 문자열은 변경되지 않습니다.
 *
 * ```kotlin
 * val tp = "orders-2".toTopicPartition()
 * // tp.topic() == "orders"
 * // tp.partition() == 2
 * ```
 */
fun String.toTopicPartition(): TopicPartition = topicPartitionOf(this)

/**
 * `"topic-partition"` 형식 문자열을 [TopicPartition]으로 파싱합니다.
 *
 * ## 동작/계약
 * - [tp]가 blank면 `assertNotBlank("tp")` 검증으로 예외가 발생합니다.
 * - 마지막 `-`를 구분자로 사용해 topic/partition을 분리합니다.
 * - 구분자가 없거나 partition이 정수가 아니면 `IllegalArgumentException`/`NumberFormatException`이 발생합니다.
 *
 * ```kotlin
 * val tp = topicPartitionOf("payments-7")
 * // tp.topic() == "payments"
 * // tp.partition() == 7
 * ```
 */
fun topicPartitionOf(tp: String): TopicPartition {
    tp.assertNotBlank("tp")

    val index = tp.lastIndexOf('-')
    if (index < 0) {
        throw IllegalArgumentException("Not found kafka topic-position delimiter (-)")
    } else {
        val topic = tp.substring(0, index)
        val partition = tp.substring(index + 1)
        return TopicPartition(topic, partition.toInt())
    }
}
