package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.uninitialized
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 소비자가 준비되지 않은 동안 들어온 값을 드롭하는 backpressure 전략을 적용합니다.
 *
 * ## 동작/계약
 * - 소비자 준비 플래그가 `true`일 때만 값을 슬롯에 저장하고 방출 준비 신호를 보냅니다.
 * - 소비자가 바쁠 때 들어온 값은 버퍼링하지 않고 폐기됩니다.
 * - upstream 예외는 그대로 전파되고, 정상 완료 시 마지막 전달값 이후 종료됩니다.
 * - 내부적으로 단일 슬롯 원자 변수와 `Resumable` 신호를 사용합니다.
 *
 * ```kotlin
 * val result = source.onBackpressureDrop().toList()
 * // 느린 collector에서는 일부 값이 드롭될 수 있다.
 * ```
 */
fun <T> Flow<T>.onBackpressureDrop(): Flow<T> = onBackpressureDropInternal(this)

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

private class OnBackpressureDropState<T> {
    val consumerReady = atomic(false)
    val value = atomic(uninitialized<T>())
    val done = atomic(false)
    val error = atomic<Throwable?>(null)
}
