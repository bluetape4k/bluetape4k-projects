package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * 모든 요소를 동일한 상수값으로 변환합니다.
 *
 * ## 동작/계약
 * - upstream 요소 개수/순서는 유지하고 값만 `value`로 치환합니다.
 * - 요소당 `emit(value)` 1회만 수행합니다.
 * - `value` 자체는 복사하지 않고 동일 참조를 재사용합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).mapTo("x").toList()
 * // result == ["x", "x", "x"]
 * ```
 *
 * @param value 각 요소를 대체할 상수값입니다.
 */
fun <T, R> Flow<T>.mapTo(value: R): Flow<R> = transform { emit(value) }

/**
 * 모든 요소를 `Unit`으로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `mapTo(Unit)`에 위임합니다.
 * - 요소 개수/순서는 유지하며 값만 `Unit`으로 치환됩니다.
 *
 * ```kotlin
 * val result = flowOf(10, 20).mapToUnit().toList()
 * // result == [Unit, Unit]
 * ```
 */
fun <T> Flow<T>.mapToUnit(): Flow<Unit> = mapTo(Unit)
