package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 이전 inner Flow가 완료될 때까지 새 upstream 값을 드롭하는 flatMap 연산입니다.
 *
 * ## 동작/계약
 * - 현재 inner Flow가 실행 중이면 새 upstream 값은 변환하지 않고 버립니다.
 * - 내부 구현은 [flatMapFirst]에 위임됩니다.
 * - backpressure 버퍼를 두지 않고 드롭 전략으로 동작합니다.
 *
 * ```kotlin
 * val source = flowOf(1, 2, 3)
 * val out = source.flatMapDrop { flowOf(it, it * 10) }
 * // out == [1, 10, ...] (첫 inner 완료 전 값은 드롭 가능)
 * ```
 *
 * @param transform upstream 값을 inner Flow로 변환하는 함수입니다.
 */
fun <T, R> Flow<T>.flatMapDrop(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    flatMapFirst(transform)
