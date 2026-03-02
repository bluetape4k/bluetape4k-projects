package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.utils.NULL_VALUE
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * source 값이 들어올 때마다 other의 최신 값과 결합합니다.
 *
 * ## 동작/계약
 * - other를 별도 코루틴에서 수집해 최신 값을 원자 참조에 저장합니다.
 * - other가 아직 한 번도 emit하지 않았다면 source 값은 방출되지 않습니다.
 * - other가 `null`을 emit한 경우 `NULL_VALUE` sentinel로 구분해 정상 결합합니다.
 * - source 또는 other 예외는 하류로 전파됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2).withLatestFrom(flowOf("A")) { a, b -> "$a$b" }.toList()
 * // result == ["1A", "2A"]
 * ```
 *
 * @param other 최신 값을 제공할 보조 Flow입니다.
 * @param transform source 값과 other 최신 값을 결합하는 함수입니다.
 */
fun <A, B, R> Flow<A>.withLatestFrom(
    other: Flow<B>,
    transform: suspend (A, B) -> R,
): Flow<R> = flow {
    val state = WithLatestFromState()

    try {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                other.collect { state.otherRef.value = it ?: NULL_VALUE }
            }

            collect { value: A ->
                emit(
                    transform(value, NULL_VALUE.unbox(state.otherRef.value ?: return@collect))
                )
            }
        }
    } finally {
        state.otherRef.value = null
    }
}

/**
 * source 값과 other 최신 값을 `Pair`로 결합합니다.
 *
 * ## 동작/계약
 * - 동작 규칙은 [withLatestFrom]과 동일합니다.
 * - 결과는 `(sourceValue, latestOtherValue)` 형태로 방출됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2).withLatestFrom(flowOf("A")).toList()
 * // result == [(1, A), (2, A)]
 * ```
 *
 * @param other 최신 값을 제공할 보조 Flow입니다.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <A, B> Flow<A>.withLatestFrom(other: Flow<B>): Flow<Pair<A, B>> =
    withLatestFrom(other) { a, b -> a to b }

private class WithLatestFromState {
    val otherRef = atomic<Any?>(null)
}
