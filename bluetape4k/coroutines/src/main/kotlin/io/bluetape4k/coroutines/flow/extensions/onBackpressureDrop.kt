package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

internal fun <T> onBackpressureDropInternal(source: Flow<T>): Flow<T> = flow {
    coroutineScope {
        val producerReady = Resumable()
        val consumerReady = AtomicBoolean(false)
        val value = AtomicReference<T>(uninitialized())
        val done = AtomicBoolean(false)
        val error = AtomicReference<Throwable>(null)

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                source.collect { item ->
                    if (consumerReady.get()) {
                        value.lazySet(item)
                        consumerReady.set(false)
                        producerReady.resume()
                    }
                }
                done.set(true)
            } catch (e: Throwable) {
                error.set(e)
            }
            producerReady.resume()
        }

        while (true) {
            consumerReady.set(true)
            producerReady.await()

            error.get()?.let { throw it }

            if (done.get()) {
                break
            }

            emit(value.getAndSet(uninitialized()))
        }
    }
}
