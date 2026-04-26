package io.bluetape4k.pulsar.producer

import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.TypedMessageBuilder

/**
 * 메시지를 비동기로 발행하고 [MessageId]를 반환합니다.
 *
 * ```kotlin
 * val msgId = producer.sendSuspend("hello")
 * ```
 *
 * @param message 발행할 메시지
 * @return 발행된 메시지의 [MessageId]
 * @throws org.apache.pulsar.client.api.PulsarClientException 브로커 오류 시
 */
suspend fun <T> Producer<T>.sendSuspend(message: T): MessageId =
    sendAsync(message).awaitSuspending()

/**
 * [TypedMessageBuilder] DSL 기반으로 메시지를 발행합니다.
 *
 * ```kotlin
 * val msgId = producer.sendSuspend {
 *     value(order)
 *     key("order-${order.id}")
 *     property("version", "1")
 * }
 * ```
 *
 * @param setup [TypedMessageBuilder] 설정 블록
 * @return 발행된 메시지의 [MessageId]
 * @throws org.apache.pulsar.client.api.PulsarClientException 브로커 오류 시
 */
suspend fun <T> Producer<T>.sendSuspend(
    setup: TypedMessageBuilder<T>.() -> Unit,
): MessageId = newMessage().apply(setup).sendAsync().awaitSuspending()

/**
 * [Flow] 기반으로 메시지를 배치 발행하고 [MessageId] Flow를 반환합니다.
 *
 * 첫 번째 메시지 발행 실패 시 Flow가 즉시 종료되고 예외가 전파됩니다.
 * 재시도는 호출자가 `Flow.retry {}` 등으로 처리해야 합니다.
 *
 * `Flow.map { }` 람다는 suspend 컨텍스트이므로 `sendSuspend` 호출이 가능합니다.
 *
 * ```kotlin
 * val ids = producer.sendAsFlow(flow {
 *     repeat(100) { emit("msg-$it") }
 * }).toList()
 * ```
 *
 * @param messages 발행할 메시지 [Flow]
 * @return 발행 결과 [MessageId] [Flow]
 */
fun <T> Producer<T>.sendAsFlow(messages: Flow<T>): Flow<MessageId> =
    messages.map { sendSuspend(it) }
