package io.bluetape4k.kafka.spring.support

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.support.KafkaUtils

/**
 * [ProducerRecord]를 포맷된 문자열로 변환합니다.
 *
 * 로깅이나 디버깅 목적으로 ProducerRecord의 내용을 읽기 쉬운 형태로 변환합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val record = ProducerRecord("topic", "key", "value")
 * println(record.prettyString())
 * // 출력: ProducerRecord(topic=topic, partition=null, timestamp=null, key=key, value=value, headers=RecordHeaders(headers = [], isReadOnly = false))
 * ```
 *
 * @return 포맷된 문자열 표현
 */
fun ProducerRecord<*, *>.prettyString(): String = KafkaUtils.format(this)

/**
 * [ConsumerRecord]를 포맷된 문자열로 변환합니다.
 *
 * 로깅이나 디버깅 목적으로 ConsumerRecord의 내용을 읽기 쉬운 형태로 변환합니다.
 *
 * 사용 예시:
 * ```kotlin
 * consumer.poll(Duration.ofSeconds(1)).forEach { record ->
 *     println(record.prettyString())
 *     // 출력: ConsumerRecord(topic = my-topic, partition = 0, offset = 42, key = my-key, value = my-value)
 * }
 * ```
 *
 * @return 포맷된 문자열 표현
 */
fun ConsumerRecord<*, *>.prettyString(): String = KafkaUtils.format(this)
