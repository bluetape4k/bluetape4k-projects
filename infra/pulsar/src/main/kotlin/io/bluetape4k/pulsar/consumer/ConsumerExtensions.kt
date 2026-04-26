package io.bluetape4k.pulsar.consumer

import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message

/**
 * 메시지를 비동기로 수신합니다.
 *
 * ```kotlin
 * val msg = consumer.receiveSuspend()
 * println(msg.value)
 * ```
 *
 * @return 수신된 [Message]
 * @throws org.apache.pulsar.client.api.PulsarClientException 브로커 오류 시
 */
suspend fun <T> Consumer<T>.receiveSuspend(): Message<T> =
    receiveAsync().awaitSuspending()

/**
 * 메시지를 무한 소비하는 [Flow]를 반환합니다.
 *
 * ## 생명주기 계약
 * - 코루틴 취소 시 대기 중인 [java.util.concurrent.CompletableFuture]를 `cancel(true)`로 중단 후 종료
 * - Flow는 Consumer를 소유하지 않음 — `withConsumer {}` 또는 호출자가 close 책임
 * - 브로커 연결 끊김 시 Pulsar Client가 자동 재연결을 시도하며, 복구 전까지 `receiveAsync()`가 블로킹됨
 *
 * ```kotlin
 * consumer.receiveAsFlow()
 *     .map { msg -> process(msg.value).also { consumer.acknowledgeSuspend(msg) } }
 *     .collect()
 * ```
 *
 * @return 수신 메시지 [Flow]
 */
fun <T> Consumer<T>.receiveAsFlow(): Flow<Message<T>> = flow {
    while (currentCoroutineContext().isActive) {
        val future = receiveAsync()
        try {
            emit(future.awaitSuspending())
        } catch (ce: CancellationException) {
            future.cancel(true)
            throw ce
        }
    }
}

/**
 * 메시지를 비동기로 ack 처리합니다.
 *
 * ```kotlin
 * consumer.acknowledgeSuspend(msg)
 * ```
 *
 * @param message ack 처리할 메시지
 * @throws org.apache.pulsar.client.api.PulsarClientException ack 전송 실패 시
 */
suspend fun <T> Consumer<T>.acknowledgeSuspend(message: Message<T>) {
    acknowledgeAsync(message).awaitSuspending()
}

/**
 * 메시지 ID까지 누적 ack 처리합니다.
 *
 * **주의**: Exclusive/Failover subscription에서만 유효합니다.
 * Shared subscription에서 호출하면 [org.apache.pulsar.client.api.PulsarClientException]이 발생합니다.
 *
 * ```kotlin
 * val msgs = (1..10).map { consumer.receiveSuspend() }
 * // 마지막 메시지 ID까지 일괄 ack — 이전 메시지 모두 포함
 * consumer.acknowledgeCumulativeSuspend(msgs.last())
 * ```
 *
 * @param message 누적 ack 기준 메시지
 * @throws org.apache.pulsar.client.api.PulsarClientException Shared 구독에서 호출 시, 또는 ack 전송 실패 시
 */
suspend fun <T> Consumer<T>.acknowledgeCumulativeSuspend(message: Message<T>) {
    acknowledgeCumulativeAsync(message).awaitSuspending()
}
