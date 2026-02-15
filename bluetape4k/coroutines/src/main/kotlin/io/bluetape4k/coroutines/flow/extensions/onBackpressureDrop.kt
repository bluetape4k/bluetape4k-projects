package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.uninitialized
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Backpressure 발생 시, item을 버린다. conflate 와 같은 동작을 수행한다.
 *
 * ```
 * flowRangeOf(0, 10)
 *      .onEach { delay(100L) }.log("source", log)
 *      .onBackpressureDrop()
 *      // .buffer(2) // buffering 하면 drop을 하지 않습니다.
 *      .onEach { delay(130L) }.log("backpressure", log)
 *      .assertResult(0, 2, 4, 6, 8)
 * ```
 *
 * @see [kotlinx.coroutines.flow.conflate]
 * @see [kotlinx.coroutines.flow.debounce]
 * @see [kotlinx.coroutines.flow.sample]
 * @see [io.bluetape4k.coroutines.flow.extensions.throttleLeading]
 * @see [io.bluetape4k.coroutines.flow.extensions.throttleTrailing]
 * @see [io.bluetape4k.coroutines.flow.extensions.throttleBoth]
 */
fun <T> Flow<T>.onBackpressureDrop(): Flow<T> = onBackpressureDropInternal(this)

/**
 * 소비자가 준비되지 않았을 때 들어온 최신 값만 유지하고 나머지는 버립니다.
 */
internal fun <T> onBackpressureDropInternal(source: Flow<T>): Flow<T> = flow {
    coroutineScope {
        val producerReady = Resumable()
        val state = OnBackpressureDropState<T>()

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                source.collect { item ->
                    if (state.consumerReady.value) {
                        state.value.lazySet(item)
                        state.consumerReady.value = false
                        producerReady.resume()
                    }
                }
                state.done.value = true
            } catch (e: Throwable) {
                state.error.value = e
            }
            producerReady.resume()
        }

        while (true) {
            state.consumerReady.value = true
            producerReady.await()

            state.error.value?.let { throw it }

            if (state.done.value) {
                break
            }

            emit(state.value.getAndSet(uninitialized()))
        }
    }
}

/**
 * [onBackpressureDropInternal]에서 producer/consumer 동기화 상태를 보관합니다.
 */
private class OnBackpressureDropState<T> {
    val consumerReady = atomic(false)
    val value = atomic(uninitialized<T>())
    val done = atomic(false)
    val error = atomic<Throwable?>(null)
}
