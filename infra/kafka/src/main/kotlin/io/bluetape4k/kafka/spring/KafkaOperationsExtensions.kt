package io.bluetape4k.kafka.spring

import kotlinx.coroutines.future.await
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.support.SendResult
import org.springframework.messaging.Message

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend(producerRecordOf("topic", "key", "value"))
 * ```
 *
 * @param record 전송할 [ProducerRecord]
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(record: ProducerRecord<K, V>): SendResult<K, V> =
    send(record).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend(messageOf("topic", "value"))
 * ```
 *
 * @param message 전송할 [Message]
 * @return [SendResult] 발송 결과
 * @see Message
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(message: Message<*>): SendResult<K, V> =
    send(message).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```kotlin
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend("my-topic", "hello")
 * // result.recordMetadata.topic() == "my-topic"
 * ```
 *
 * @param topic 발송할 토픽
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(topic: String, value: V): SendResult<K, V> =
    send(topic, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```kotlin
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend("my-topic", "my-key", "hello")
 * // result.recordMetadata.topic() == "my-topic"
 * ```
 *
 * @param topic 발송할 토픽
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(topic: String, key: K, value: V): SendResult<K, V> =
    send(topic, key, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```kotlin
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend("my-topic", partition = 0, key = "my-key", value = "hello")
 * // result.recordMetadata.partition() == 0
 * ```
 *
 * @param topic 발송할 토픽
 * @param partition 발송할 파티션
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(
    topic: String,
    partition: Int,
    key: K,
    value: V,
): SendResult<K, V> =
    send(topic, partition, key, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```kotlin
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.suspendSend(
 *     topic = "my-topic",
 *     partition = 0,
 *     timestamp = System.currentTimeMillis(),
 *     key = "my-key",
 *     value = "hello"
 * )
 * // result.recordMetadata.partition() == 0
 * ```
 *
 * @param topic 발송할 토픽
 * @param partition 발송할 파티션
 * @param timestamp 발송할 타임스탬프
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSend(
    topic: String,
    partition: Int,
    timestamp: Long,
    key: K,
    value: V,
): SendResult<K, V> =
    send(topic, partition, timestamp, key, value).await()

/**
 * [KafkaOperations] 기본 Topic으로 발송하는 작업을 suspend 함수로 실행합니다.
 *
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 * @see KafkaOperations.sendDefault
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSendDefault(value: V): SendResult<K, V> =
    sendDefault(value).await()

/**
 * [KafkaOperations] 기본 Topic으로 발송하는 작업을 suspend 함수로 실행합니다.
 *
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 * @see KafkaOperations.sendDefault
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.suspendSendDefault(key: K, value: V): SendResult<K, V> =
    sendDefault(key, value).await()
