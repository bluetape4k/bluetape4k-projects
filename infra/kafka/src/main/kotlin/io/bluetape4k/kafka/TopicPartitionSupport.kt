package io.bluetape4k.kafka

import io.bluetape4k.support.assertNotBlank
import org.apache.kafka.common.TopicPartition

/**
 * 지정된 문자열을 [TopicPartition]으로 변환합니다.
 *
 * ```
 * val tp = "test-0".toTopicPartition()
 * ```
 *
 * @receiver 변환할 문자열
 * @return [TopicPartition] 인스턴스
 */
fun String.toTopicPartition(): TopicPartition = topicPartitionOf(this)

/**
 * 지정된 문자열을 [TopicPartition]으로 변환합니다.
 *
 * ```
 * val tp = topicPartitionOf("test.topic.1-3")  // topic: test.topic.1, partition: 3
 * ```
 *
 * @param tp 변환할 문자열
 * @return [TopicPartition] 인스턴스
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
//    val (topic, partition) = tp.split("-", limit = 2)
//    return TopicPartition(topic, partition.toInt())
}
