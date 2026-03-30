package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * upstream collect를 지정 dispatcher에서 시작하도록 전환합니다.
 *
 * ## 동작/계약
 * - upstream collect를 별도 `launch(dispatcher)`에서 실행하고, 현재 collector로는 버퍼를 통해 전달합니다.
 * - downstream 컨텍스트는 유지하면서 upstream 시작 지점만 분리합니다.
 * - upstream 예외는 `ResumableCollector`를 통해 downstream으로 전파됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).startCollectOn(Dispatchers.IO)
 * // out은 원소 순서를 유지해 1, 2, 3을 방출
 * ```
 *
 * @param dispatcher upstream collect를 시작할 dispatcher입니다.
 */
fun <T> Flow<T>.startCollectOn(dispatcher: CoroutineDispatcher): Flow<T> =
    startCollectOnInternal(this, dispatcher)

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                inner.error(e)
            }
        }

        inner.drain(this@flow)
    }
}
