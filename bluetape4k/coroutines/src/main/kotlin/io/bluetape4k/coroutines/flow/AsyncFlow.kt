package io.bluetape4k.coroutines.flow

import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@PublishedApi
internal class LazyDeferred<out T>(
    val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    @BuilderInference val block: suspend CoroutineScope.() -> T,
) {
    private val deferred = atomic<Deferred<T>?>(null)

    fun start(scope: CoroutineScope): LazyDeferred<T> {
        deferred.compareAndSet(null, scope.async(coroutineContext, block = block))
        return this
    }

    suspend fun await(): T = deferred.value?.await() ?: error("Coroutine not started")
}

/**
 * Flow 요소를 비동기 계산으로 감싼 뒤, 원래 순서대로 방출하는 Flow 래퍼입니다.
 *
 * ## 동작/계약
 * - 각 요소 계산은 `LazyDeferred`로 비동기 시작되지만, 최종 emit은 입력 순서를 유지합니다.
 * - 수신 객체를 변경하지 않고 새 Flow 래퍼를 반환합니다.
 * - 계산 중 예외는 해당 요소 await 시점에 전파됩니다.
 * - 내부 채널/Deferred를 사용하므로 요소 수와 버퍼 크기에 비례한 추가 할당이 발생합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .async { it * 2 }
 *     .toList()
 * // out == [2, 4, 6]
 * ```
 */
class AsyncFlow<T> @PublishedApi internal constructor(
    @PublishedApi internal val deferredFlow: Flow<LazyDeferred<T>>,
): AbstractFlow<T>() {

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        channelFlow {
            deferredFlow.collect { defer ->
                send(defer.start(this))
            }
        }.collect { defer ->
            collector.emit(defer.await())
        }
    }
}

@PublishedApi
internal fun requireFlowBufferCapacity(capacity: Int) {
    when (capacity) {
        Channel.BUFFERED, Channel.CONFLATED -> Unit
        else                                -> capacity.requireZeroOrPositiveNumber("capacity")
    }
}

/**
 * Flow 각 요소를 비동기 계산으로 변환한 [AsyncFlow]를 생성합니다.
 *
 * ## 동작/계약
 * - 각 입력 요소마다 [block]을 별도 Deferred로 계산합니다.
 * - 계산은 비동기적으로 시작되지만 결과 방출 순서는 입력 순서를 유지합니다.
 * - [coroutineContext]는 요소 계산 coroutine 생성 시 적용됩니다.
 * - block 예외는 collect 시점에 전파됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .async { it + 1 }
 *     .toList()
 * // out == [2, 3, 4]
 * ```
 * @param coroutineContext 요소 계산에 사용할 CoroutineContext입니다.
 * @param block 각 요소를 비동기 계산하는 함수입니다.
 */
inline fun <T, R> Flow<T>.async(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    @BuilderInference crossinline block: suspend CoroutineScope.(T) -> R,
): AsyncFlow<R> {
    val deferredFlow: Flow<LazyDeferred<R>> = map { input ->
        LazyDeferred(coroutineContext) { block(input) }
    }

    return AsyncFlow(deferredFlow)
}

/**
 * [AsyncFlow] 결과를 추가 변환한 새 [AsyncFlow]를 반환합니다.
 *
 * ## 동작/계약
 * - 기존 비동기 계산을 시작/await한 뒤 [transform]을 적용합니다.
 * - 입력 순서 보장은 유지됩니다.
 * - transform 예외는 collect 시점에 전파됩니다.
 * - 수신 AsyncFlow를 변경하지 않고 새 AsyncFlow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .async { it }
 *     .map { it * 10 }
 *     .toList()
 * // out == [10, 20, 30]
 * ```
 * @param transform AsyncFlow 요소 변환 함수입니다.
 */
inline fun <T, R> AsyncFlow<T>.map(
    @BuilderInference crossinline transform: suspend (value: T) -> R,
): AsyncFlow<R> {
    return AsyncFlow(
        deferredFlow
            .map { input ->
                LazyDeferred(input.coroutineContext) {
                    input.start(this)
                    transform(input.await())
                }
            }
    )
}

/**
 * [AsyncFlow]를 지정한 버퍼 정책으로 수집합니다.
 *
 * ## 동작/계약
 * - `capacity`는 `Channel.BUFFERED`, `Channel.CONFLATED` 또는 0 이상이어야 합니다.
 * - 조건을 만족하지 않으면 `IllegalArgumentException`이 발생합니다.
 * - 내부적으로 deferredFlow를 buffer한 뒤 각 값을 await하여 [collector]에 전달합니다.
 * - `collector`를 생략하면 값을 소비만 하고 버립니다.
 *
 * ```kotlin
 * val count = AtomicInteger(0)
 * flowOf(1, 2, 3).async { it }.collect { count.incrementAndGet() }
 * // count.get() == 3
 * ```
 * @param capacity 내부 buffer 크기 또는 채널 특수 상수입니다.
 * @param collector 수집된 값을 처리할 collector입니다.
 */
suspend fun <T> AsyncFlow<T>.collect(
    capacity: Int = Channel.BUFFERED,
    collector: FlowCollector<T> = NoopCollector,
) {
    requireFlowBufferCapacity(capacity)

    channelFlow {
        deferredFlow
            .buffer(capacity)
            .collect { defer ->
                defer.start(this)
                send(defer)
            }
    }.collect { defer ->
        collector.emit(defer.await())
    }
}

/**
 * [AsyncFlow]를 람다 collector로 수집합니다.
 *
 * ## 동작/계약
 * - 동작은 [collect](`capacity`, `FlowCollector`)와 동일합니다.
 * - `capacity` 검증 규칙도 동일하게 적용됩니다.
 * - collector 람다 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val out = mutableListOf<Int>()
 * flowOf(1, 2, 3).async { it }.collect { out += it }
 * // out == [1, 2, 3]
 * ```
 * @param capacity 내부 buffer 크기 또는 채널 특수 상수입니다.
 * @param collector 수집된 값을 처리할 람다입니다.
 */
suspend inline fun <T> AsyncFlow<T>.collect(
    capacity: Int = Channel.BUFFERED,
    @BuilderInference crossinline collector: suspend (value: T) -> Unit,
) {
    requireFlowBufferCapacity(capacity)
    collect(capacity, FlowCollector { value -> collector(value) })
}
