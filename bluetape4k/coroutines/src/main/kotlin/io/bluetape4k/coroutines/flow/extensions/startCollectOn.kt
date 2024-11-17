package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 지정된 [dispatcher]에서 upstream 을 수집합니다.
 *
 * ```
 * val four = newFixedThreadPoolContext(4, "four")
 * val single = newSingleThreadContext("single")
 *
 * flowRangeOf(1, 10)
 *     .startCollectOn(four)
 *     .flowOn(single)
 *     .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 *
 * flowRangeOf(1, 10)
 *     .buffer(4)
 *     .startCollectOn(four)
 *     .flowOn(single)
 *     .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * ```
 *
 * @param dispatcher 수집할 dispatcher
 */
fun <T> Flow<T>.startCollectOn(dispatcher: CoroutineDispatcher): Flow<T> =
    startCollectOnInternal(this, dispatcher)
// FlowStartCollectOn(this, dispatcher)

/**
 * 지정된 [dispatcher]에서 [source] 을 수집합니다.
 */
internal fun <T> startCollectOnInternal(
    source: Flow<T>,
    dispatcher: CoroutineDispatcher,
): Flow<T> = flow {
    coroutineScope {
        val inner = ResumableCollector<T>()

        launch(dispatcher) {
            try {
                source.collect {
                    inner.next(it)
                }
                inner.complete()
            } catch (e: Throwable) {
                inner.error(e)
            }
        }

        inner.drain(this@flow)
    }
}
