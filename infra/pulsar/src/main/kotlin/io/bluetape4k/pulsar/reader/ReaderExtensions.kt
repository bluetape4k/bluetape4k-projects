package io.bluetape4k.pulsar.reader

import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.Reader

/**
 * 다음 메시지를 비동기로 읽습니다.
 *
 * ```kotlin
 * val msg = reader.readNextSuspend()
 * println(msg.value)
 * ```
 *
 * @return 읽은 [Message]
 * @throws org.apache.pulsar.client.api.PulsarClientException 브로커 오류 시
 */
suspend fun <T> Reader<T>.readNextSuspend(): Message<T> =
    readNextAsync().awaitSuspending()

/**
 * `hasMessageAvailable()`이 true인 동안 메시지를 읽는 [Flow]를 반환합니다.
 *
 * ## 생명주기 계약
 * - `hasMessageAvailable() == false`이면 Flow 정상 종료
 * - 코루틴 취소 시 대기 중인 [java.util.concurrent.CompletableFuture]를 `cancel(true)`로 중단 후 종료
 * - Flow는 Reader를 소유하지 않음 — `withReader {}` 또는 호출자가 close 책임
 *
 * ```kotlin
 * client.withReader(Schema.STRING, { topic(topic); startMessageId(MessageId.earliest) }) {
 *     readAsFlow().map { it.value }.collect { println(it) }
 * }
 * ```
 *
 * @return 메시지 [Flow] (발행된 메시지를 모두 읽으면 자동 종료)
 */
fun <T> Reader<T>.readAsFlow(): Flow<Message<T>> = flow {
    while (currentCoroutineContext().isActive && hasMessageAvailable()) {
        val future = readNextAsync()
        try {
            emit(future.awaitSuspending())
        } catch (ce: CancellationException) {
            future.cancel(true)
            throw ce
        }
    }
}
