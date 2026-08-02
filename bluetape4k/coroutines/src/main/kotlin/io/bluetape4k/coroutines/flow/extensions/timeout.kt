package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/** Flow가 지정된 idle 시간 안에 원소를 방출하지 않았을 때 사용하는 예외입니다. */
class FlowTimeoutException(
    val timeout: Duration,
): TimeoutException("Flow did not emit within $timeout")

/**
 * collection 시작 후 [timeout] 동안 새 원소가 없으면 [FlowTimeoutException]을
 * 방출하는 idle-timeout 연산자입니다.
 *
 * ## 동작/계약
 * - timer는 collection 시작과 동시에 시작하고, 각 원소를 downstream으로
 *   방출한 뒤 다시 시작합니다.
 * - 정상 완료가 pending timeout보다 먼저 관찰되면 원소를 그대로 완료합니다.
 * - timeout 시 upstream을 먼저 취소하고 cleanup이 끝난 뒤 예외를 방출합니다.
 * - caller의 `CancellationException`은 timeout 예외로 변환하지 않습니다.
 *
 * ```kotlin
 * val result = source.timeout(500.milliseconds).toList()
 * ```
 *
 * @param timeout 허용할 최대 idle 시간입니다.
 */
fun <T> Flow<T>.timeout(timeout: Duration): Flow<T> =
    timeoutInternal(timeout, fallback = null)

/**
 * collection 시작 후 idle timeout이 발생하면 upstream cleanup 뒤 [fallback]을
 * 정확히 한 번 수집하는 연산자입니다.
 *
 * upstream 정상 완료와 upstream/fallback 실패는 원래 의미 그대로 전달되며,
 * caller 취소는 항상 cancellation으로 유지됩니다.
 *
 * ```kotlin
 * val result = source.timeoutOrFallback(500.milliseconds, cached).toList()
 * ```
 *
 * @param timeout 허용할 최대 idle 시간입니다.
 * @param fallback timeout 이후 한 번 수집할 cold fallback Flow입니다.
 */
fun <T> Flow<T>.timeoutOrFallback(timeout: Duration, fallback: Flow<T>): Flow<T> =
    timeoutInternal(timeout, fallback)

private fun <T> Flow<T>.timeoutInternal(timeout: Duration, fallback: Flow<T>?): Flow<T> = flow {
    require(timeout.isPositive()) { "timeout must be positive" }

    supervisorScope {
        val input = Channel<T>(Channel.BUFFERED)
        val upstream = launch {
            try {
                this@timeoutInternal.collect { input.send(it) }
                input.close()
            } catch (cause: Throwable) {
                input.close(cause)
            }
        }

        var timedOut = false
        try {
            whileSelect {
                input.onReceiveCatching { result ->
                    result
                        .onSuccess { emit(it) }
                        .onFailure { cause -> cause?.let { throw it } }
                        .isSuccess
                }
                onTimeout(timeout) {
                    timedOut = true
                    upstream.cancelAndJoin()
                    input.cancel()
                    false
                }
            }
        } finally {
            if (!timedOut) upstream.cancelAndJoin()
            input.cancel()
        }

        if (timedOut) {
            if (fallback == null) {
                throw FlowTimeoutException(timeout)
            }
            emitAll(fallback)
        }
    }
}
