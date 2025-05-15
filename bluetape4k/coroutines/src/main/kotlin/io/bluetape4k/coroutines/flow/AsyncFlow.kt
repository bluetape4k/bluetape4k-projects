package io.bluetape4k.coroutines.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [block]을 [start]를 호출할 때까지 실행하지 않는 [Deferred]를 생성합니다.
 *
 * @param T 반환할 값의 수형
 * @property coroutineContext 비동기 실행을 위한 [CoroutineContext]
 * @property block 비동기로 실행할 suspend 함수
 */
@PublishedApi
internal class LazyDeferred<out T>(
    val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    val block: suspend CoroutineScope.() -> T,
) {
    private val deferred = AtomicReference<Deferred<T>?>(null)

    /**
     * [block]을 비동기 방식으로 실행합니다.
     *
     * @param scope [block]을 실행할 [CoroutineScope]
     */
    fun start(scope: CoroutineScope): LazyDeferred<T> {
        deferred.compareAndSet(null, scope.async(coroutineContext, block = block))
        return this
    }

    /**
     * [Deferred]의 실행이 완료될 때까지 대기하고, 결과를 반환합니다. deferred 가 실행되지 않았으면 에러를 발생시킵니다.
     */
    suspend fun await(): T = deferred.get()?.await() ?: error("Coroutine not started")
}

/**
 * Flow 형식을 취하지만 요소가 [LazyDeferred] 형식을 emit 하는 Flow 입니다.
 *
 * ```
 * (0..10).asFlow()
 *     .asyncFlow {
 *          delay(100)
 *          it * 2
 *     }
 *     .collect { item: Int ->
 *         println(item)
 *     }
 * ```
 *
 * @param T 요소의 수형
 * @property deferredFlow [LazyDeferred]를 emit 하는 [Flow] 인스턴스
 */
class AsyncFlow<out T> @PublishedApi internal constructor(
    @PublishedApi internal val deferredFlow: Flow<LazyDeferred<T>>,
): Flow<T> {

    /**
     * [Flow]와 마찮가지로 emit된 요소를 collect 합니다.
     *
     * @param collector emit 된 요소를 collect 하는 [FlowCollector]
     */
    override suspend fun collect(collector: FlowCollector<T>) {
        channelFlow {
            deferredFlow.collect { defer ->
                send(defer.start(this))
            }
        }.collect { defer ->
            collector.emit(defer.await())
        }
    }
}

/**
 * [Flow] 를 [AsyncFlow] 로 변환하여, 각 요소처리를 병렬로 비동기 방식으로 수행하게 합니다.
 * 단 `flatMapMerge` 처럼 실행 완료된 순서로 반환하는 것이 아니라, Flow 의 처음 요소의 순서대로 반환합니다. (Deferred 형식으로)
 *
 * ```
 * val dispatcher = Dispatchers.Default
 *
 * expectedItems.asFlow()
 *     .async(dispatcher) {
 *         delay(Random.nextLong(5))
 *         log.trace { "Started $it" }
 *         it
 *     }
 *     .map {
 *         delay(Random.nextLong(5))
 *         it * it / it
 *     }
 *     .collect { curr ->
 *         // 순서대로 들어와야 한다
 *         results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
 *         results.add(curr)
 *     }
 * ```
 */
inline fun <T, R> Flow<T>.async(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend CoroutineScope.(T) -> R,
): AsyncFlow<R> {
    val deferredFlow = map { input -> LazyDeferred(coroutineContext) { block(input) } }
    return AsyncFlow(deferredFlow)
}

/**
 * [AsyncFlow] 의 요소들을 병렬로 비동기로 [transform]을 실행하지만, 원본 Flow의 순서를 유지합니다.
 *
 * ```
 * expectedItems.asFlow()
 *     .async(dispatcher) {
 *         delay(Random.nextLong(5))
 *         log.trace { "Started $it" }
 *         it
 *     }
 *     .map {
 *         delay(Random.nextLong(5))
 *         it * it / it
 *     }
 *     .collect { curr ->
 *         // 순서대로 들어와야 한다
 *         results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
 *         results.add(curr)
 *     }
 * ```
 */
inline fun <T, R> AsyncFlow<T>.map(
    crossinline transform: suspend (value: T) -> R,
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
 * [AsyncFlow] 의 요소들을 병렬로 비동기로 실행하지만, 원본 Flow의 순서를 유지합니다.
 *
 * ```
 * expectedItems.asFlow()
 *     .async(dispatcher) {
 *         delay(Random.nextLong(5))
 *         log.trace { "Started $it" }
 *         it
 *     }
 *     .map {
 *         delay(Random.nextLong(5))
 *         it * it / it
 *     }
 *     .collect { curr ->
 *         // 순서대로 들어와야 한다
 *         results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
 *         results.add(curr)
 *     }
 * }
 * ```
 */
suspend fun <T> AsyncFlow<T>.collect(
    capacity: Int = Channel.BUFFERED,
    collector: FlowCollector<T> = NoopCollector,
) {
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
 * [AsyncFlow] 의 요소들을 [collector]을 통해 병렬 비동기로 실행하지만, 원본 Flow의 순서를 유지합니다.
 *
 * ```
 * expectedItems.asFlow()
 *     .async(dispatcher) {
 *         delay(Random.nextLong(5))
 *         log.trace { "Started $it" }
 *         it
 *     }
 *     .map {
 *         delay(Random.nextLong(5))
 *         it * it / it
 *     }
 *     .collect { curr ->
 *         // 순서대로 들어와야 한다
 *         results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
 *         results.add(curr)
 *     }
 * }
 * ```
 */
suspend inline fun <T> AsyncFlow<T>.collect(
    capacity: Int = Channel.BUFFERED,
    crossinline collector: suspend (value: T) -> Unit,
) {
    collect(capacity, FlowCollector { value -> collector(value) })
}
