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
 * val result = kafkaTemplate.sendSuspending(producerRecordOf("topic", "key", "value"))
 * ```
 *
 * @param record 전송할 [ProducerRecord]
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(record: ProducerRecord<K, V>): SendResult<K, V> =
    send(record).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(record)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(record: ProducerRecord<K, V>): SendResult<K, V> =
    send(record).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * ```
 * val kafkaTemplate: KafkaTemplate<String, String> = ...
 * val result = kafkaTemplate.sendSuspending(messageOf("topic", "value"))
 * ```
 *
 * @param message 전송할 [Message]
 * @return [SendResult] 발송 결과
 * @see Message
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(message: Message<*>): SendResult<K, V> =
    send(message).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(message)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(message: Message<*>): SendResult<K, V> =
    send(message).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * @param topic 발송할 토픽
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(topic: String, value: V): SendResult<K, V> =
    send(topic, value).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(topic, value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(topic: String, value: V): SendResult<K, V> =
    send(topic, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * @param topic 발송할 토픽
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(topic: String, key: K, value: V): SendResult<K, V> =
    send(topic, key, value).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(topic, key, value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(topic: String, key: K, value: V): SendResult<K, V> =
    send(topic, key, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * @param topic 발송할 토픽
 * @param partition 발송할 파티션
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(
    topic: String,
    partition: Int,
    key: K,
    value: V,
): SendResult<K, V> =
    send(topic, partition, key, value).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(topic, partition, key, value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(
    topic: String,
    partition: Int,
    key: K,
    value: V,
): SendResult<K, V> =
    send(topic, partition, key, value).await()

/**
 * [KafkaOperations] 발송을 suspend 함수로 실행합니다.
 *
 * @param topic 발송할 토픽
 * @param partition 발송할 파티션
 * @param timestamp 발송할 타임스탬프
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendAndAwait(
    topic: String,
    partition: Int,
    timestamp: Long,
    key: K,
    value: V,
): SendResult<K, V> =
    send(topic, partition, timestamp, key, value).await()

@Deprecated(
    message = "Use `sendAndAwait` instead.",
    replaceWith = ReplaceWith("sendAndAwait(topic, partition, timestamp, key, value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendSuspending(
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
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.awaitSendDefault(value: V): SendResult<K, V> =
    sendDefault(value).await()

@Deprecated(
    message = "Use `awaitSendDefault` instead.",
    replaceWith = ReplaceWith("awaitSendDefault(value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendDefaultSuspending(value: V): SendResult<K, V> =
    sendDefault(value).await()

/**
 * [KafkaOperations] 기본 Topic으로 발송하는 작업을 suspend 함수로 실행합니다.
 *
 * @param key 발송할 키
 * @param value 발송할 값
 * @return [SendResult] 발송 결과
 * @see KafkaOperations.sendDefault
 */
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.awaitSendDefault(key: K, value: V): SendResult<K, V> =
    sendDefault(key, value).await()

@Deprecated(
    message = "Use `awaitSendDefault` instead.",
    replaceWith = ReplaceWith("awaitSendDefault(key, value)")
)
suspend fun <K: Any, V: Any> KafkaOperations<K, V>.sendDefaultSuspending(key: K, value: V): SendResult<K, V> =
    sendDefault(key, value).await()
