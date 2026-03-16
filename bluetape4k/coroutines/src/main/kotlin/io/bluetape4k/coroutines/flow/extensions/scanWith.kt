package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan

/**
 * collect 시점에 초기값을 계산한 뒤 누적(scan) 결과를 방출합니다.
 *
 * ## 동작/계약
 * - collect마다 `initialSupplier()`를 1회 호출해 초기 accumulator를 만듭니다.
 * - 이후 `scan(initial, operation)` 규칙으로 중간 누적값을 순차 방출합니다.
 * - 초기값 계산/누적 중 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).scanWith({ 0 }) { acc, v -> acc + v }.toList()
 * // result == [0, 1, 3, 6]
 * ```
 *
 * @param initialSupplier 초기 누적값을 반환하는 suspend 함수입니다.
 * @param operation 누적 연산 함수입니다.
 */
fun <T, R> Flow<T>.scanWith(
    initialSupplier: suspend () -> R,
    operation: suspend (acc: R, item: T) -> R,
): Flow<R> = flow {
    emitAll(scan(initialSupplier(), operation))
}
