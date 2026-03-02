package io.bluetape4k.coroutines.flow.extensions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * 첫 inner Flow가 실행 중일 때 새 upstream 값을 무시하는 flatMap 연산입니다.
 *
 * ## 동작/계약
 * - 현재 inner 수집이 끝나기 전까지 들어오는 새 값은 `transform`이 호출되지 않고 드롭됩니다.
 * - 내부적으로 `map(transform).flattenFirst()`에 위임합니다.
 * - 드롭 전략이므로 동시 burst 입력에서 일부 값은 의도적으로 누락됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).flatMapFirst { flowOf(it, it * 10) }
 * // 첫 inner 실행 중 들어온 값은 건너뛸 수 있다.
 * ```
 *
 * @param transform 값을 inner Flow로 변환하는 함수입니다.
 */
fun <T, R> Flow<T>.flatMapFirst(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    map(transform).flattenFirst()

/**
 * [flatMapFirst]의 별칭입니다.
 *
 * ## 동작/계약
 * - 동작/드롭 규칙은 [flatMapFirst]와 동일합니다.
 *
 * @param transform 값을 inner Flow로 변환하는 함수입니다.
 */
fun <T, R> Flow<T>.exhaustMap(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    flatMapFirst(transform)

/**
 * `Flow<Flow<T>>`에 대해 첫 inner만 수집하는 exhaust 전략을 적용합니다.
 *
 * ## 동작/계약
 * - 동작은 [flattenFirst]와 동일합니다.
 */
fun <T> Flow<Flow<T>>.exhaustAll(): Flow<T> = flattenFirst()

/**
 * `Flow<Flow<T>>`에서 현재 inner가 끝날 때까지 다음 inner를 무시합니다.
 *
 * ## 동작/계약
 * - `busy=false`일 때만 inner 수집 코루틴을 시작합니다.
 * - inner 완료 또는 취소 시 `busy=false`로 복귀하여 다음 inner를 받을 수 있습니다.
 * - upstream/inner의 예외 처리 규칙은 `channelFlow` 기본 계약을 따릅니다.
 *
 * ```kotlin
 * val nested = flowOf(flowOf(1, 2), flowOf(3, 4))
 * val result = nested.flattenFirst().toList()
 * // 두 번째 inner는 첫 inner 실행 중이면 드롭될 수 있다.
 * ```
 */
fun <T> Flow<Flow<T>>.flattenFirst(): Flow<T> = channelFlow {
    val state = FlatMapFirstState()

    collect { inner ->
        if (state.busy.compareAndSet(false, true)) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    inner.collect { send(it) }
                    state.busy.value = false
                } catch (e: CancellationException) {
                    state.busy.value = false
                }
            }
        }
    }
}

private class FlatMapFirstState {
    val busy = atomic(false)
}
