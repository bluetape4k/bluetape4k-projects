package io.bluetape4k.kafka.spring.listener.adapter

import org.springframework.kafka.listener.adapter.AdapterUtils
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata

/**
 * 인자 배열로부터 [ConsumerRecordMetadata]를 추출합니다.
 *
 * 메시지 리스너 메소드의 인자들에서 ConsumerRecordMetadata를 검색합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @KafkaListener(topics = ["test-topic"])
 * fun listen(message: String, @Header(KafkaHeaders.RECEIVED_KEY) key: String) {
 *     val metadata = consumerRecordMetadataFromArray(message, key)
 *     // metadata 처리
 * }
 * ```
 *
 * @param datas 메시지 리스너 인자 배열
 * @return [ConsumerRecordMetadata] 또는 null
 */
fun consumerRecordMetadataFromArray(vararg datas: Any): Any? = AdapterUtils.buildConsumerRecordMetadataFromArray(*datas)

/**
 * 데이터 객체로부터 [ConsumerRecordMetadata]를 추출합니다.
 *
 * 주어진 데이터 객체가 ConsumerRecordMetadata를 포함하고 있으면 반환합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val metadata = consumerRecordMetadataOf(someData)
 * metadata?.let {
 *     println("Topic: ${it.topic()}, Partition: ${it.partition()}, Offset: ${it.offset()}")
 * }
 * ```
 *
 * @param data 데이터 객체
 * @return [ConsumerRecordMetadata] 또는 null
 */
fun consumerRecordMetadataOf(data: Any): ConsumerRecordMetadata? = AdapterUtils.buildConsumerRecordMetadata(data)
