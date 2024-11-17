package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan

/**
 * 주어진 flow 를 [operation]으로 fold 하고, 수집 시접의 [initialSupplier]가 제공한 값과, 중간 결과를 모두 emit 합니다.
 *
 * **반환되는 초기값은 다른 수집기 사이에서 공유되므로 변경되지 않아야 합니다.**
 *
 * 초기값을 지연 제공하는 [scan]의 변형입니다. 초기값이 생성 비용이 많이 들거나 수집 시점에 실행해야 하는 로직에 의존하는 경우 유용합니다.
 *
 * For example:
 * ```
 * flowOf(1, 2, 3)
 *     .scanWith({ emptyList<Int>() }) { acc, value -> acc + value }
 *     .toList()
 * ```
 * will produce `[[], [1], [1, 2], [1, 2, 3]]`.
 *
 * Another example:
 * ```kotlin
 * // Logic to calculate initial value (e.g. call API, read from DB, etc.)
 * suspend fun calculateInitialValue(): Int {
 *   println("calculateInitialValue")
 *   delay(1000)
 *   return 0
 * }
 *
 * flowOf(1, 2, 3).scanWith(::calculateInitialValue) { acc, value -> acc + value }
 * ```
 *
 * @param initialSupplier 지연된 초기 값을 제공하는 함수
 * @param operation 현 [Flow]로부터 발행되는 요소들을 각각의 수행기에 대해 호출되는 accumulator 함수
 */
fun <T, R> Flow<T>.scanWith(
    initialSupplier: suspend () -> R,
    @BuilderInference operation: suspend (acc: R, item: T) -> R,
): Flow<R> = flow {
    return@flow emitAll(scan(initialSupplier(), operation))
}
