package io.bluetape4k.coroutines.flow.extensions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * 이 메소드는 `map(transform).flattenFirst()` 과 같다.
 *
 * 참고: [flattenFirst].
 *
 * ### Operator fusion
 *
 * ** NOTE: [flatMapFirst] 이후에 [flowOn], [buffer], [produceIn] 를 적용하는 것은 동시에 merge 되는 문제가 발생할 수 있습니다.**
 *
 * ```
 * flowOf("one", "two").log("s")
 *     .flatMapFirst { v ->
 *         flow {
 *             delay(10L)
 *             emit(v)
 *         }.log("t")
 *     }
 *     .log("flatMapFirst")
 *     .assertResult("one")
 * ```
 *
 * @param transform flow 요소를 Flow로 변환하는 람다 식
 *
 */
fun <T, R> Flow<T>.flatMapFirst(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    map(transform).flattenFirst()

/**
 * 이 함수는 [flattenFirst] operator의 별칭입니다.
 */
fun <T, R> Flow<T>.exhaustMap(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    flatMapFirst(transform)

/**
 * 이 함수는 [flattenFirst] operator의 별칭입니다.
 */
fun <T> Flow<Flow<T>>.exhaustAll(): Flow<T> = flattenFirst()

/**
 * high-order [Flow]에서 first-order [Flow]로 반환합니다.
 * 이전 [Flow]가 아직 완료되지 않았으면 내부 [Flow]를 버립니다.
 *
 * ```
 * val flow1 = flow {
 *     delay(10L)
 *     emit("one")
 * }
 * val flow2 = flow {
 *     delay(20L)
 *     emit("two")
 * }
 * flowOf(flow1, flow2)
 *     .flattenFirst()
 *     .assertResult("one")
 * ```
 */
fun <T> Flow<Flow<T>>.flattenFirst(): Flow<T> = channelFlow {
    val state = FlatMapFirstState()

    collect { inner ->
        if (state.busy.compareAndSet(false, true)) {
            // Do not pay for dispatch here, it's never necessary
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

/**
 * [flattenFirst]의 내부 점유 상태를 보관합니다.
 */
private class FlatMapFirstState {
    val busy = atomic(false)
}
