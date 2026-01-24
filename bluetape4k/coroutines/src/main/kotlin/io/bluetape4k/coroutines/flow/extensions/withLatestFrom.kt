package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.utils.NULL_VALUE
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * 두 개의 [Flow]를 결합하여 하나의 [Flow]로 만듭니다. 각 값은 두 번째 [Flow]의 최신 값과 결합됩니다(있는 경우).
 * 두 번째 [Flow]가 값을 방출하기 전에 self에서 방출된 값은 생략됩니다.
 *
 * ```
 * val f1 = flowOf(1, 2, 3, 4)
 * val f2 = flowOf("a", "b", "c", "d", "e")
 *
 * f2.withLatestFrom(f1)
 *     .assertResult(
 *         "a" to 4,
 *         "b" to 4,
 *         "c" to 4,
 *         "d" to 4,
 *         "e" to 4
 *     )
 * ```
 *
 * @param other 두 번째 [Flow]
 * @param transform [other]의 마지막 발행된 값과 self의 값을 이용하는 변환 함수
 */
fun <A, B, R> Flow<A>.withLatestFrom(
    other: Flow<B>,
    transform: suspend (A, B) -> R,
): Flow<R> = flow {
    val otherRef = AtomicReference<Any?>(null)

    try {
        coroutineScope {
            // other 을 collect 해서 가장 최신의 값을 otherRef 에 저장하도록 한다
            launch(start = CoroutineStart.UNDISPATCHED) {
                other.collect { otherRef.set(it ?: NULL_VALUE) }
            }

            // source 로부터 값이 emit 되면 otherRef의 값과 함께 transform을 호출하도록 한다.
            // 만약 otherRef 값이 null 이라면 collect 를 중단한다
            collect { value: A ->
                emit(
                    transform(value, NULL_VALUE.unbox(otherRef.get() ?: return@collect))
                )
            }
        }
    } finally {
        otherRef.set(null)
    }
}

/**
 * 두 개의 [Flow]를 결합하여 하나의 [Flow]로 만듭니다. 각 값은 두 번째 [Flow]의 최신 값과 결합됩니다(있는 경우).
 * 두 번째 [Flow]가 값을 방출하기 전에 self에서 방출된 값은 생략됩니다.
 *
 * ```
 * val f1 = flowOf(1, 2, 3, 4)
 * val f2 = flowOf("a", "b", "c", "d", "e")
 *
 * f2.withLatestFrom(f1)
 *     .assertResult(
 *         "a" to 4,
 *         "b" to 4,
 *         "c" to 4,
 *         "d" to 4,
 *         "e" to 4
 *     )
 * ```
 *
 * @param other 두 번째 [Flow]
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <A, B> Flow<A>.withLatestFrom(other: Flow<B>): Flow<Pair<A, B>> =
    withLatestFrom(other) { a, b -> a to b }
