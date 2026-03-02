package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Flow 요소를 지정 타입으로 강제 캐스팅합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `it as R`를 수행하므로 타입이 맞지 않으면 `ClassCastException`이 발생합니다.
 * - `null` 요소를 non-null 타입 `R`로 캐스팅하면 예외가 발생할 수 있습니다.
 * - 요소 개수와 순서는 유지됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).cast<Int>().toList()
 * // result == [1, 2, 3]
 * ```
 */
inline fun <reified R> Flow<*>.cast(): Flow<R> = map { it as R }

/**
 * Flow 요소를 안전 캐스팅하고 실패한 요소는 제외합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `it as? R`를 사용해 캐스팅 성공 요소만 통과시킵니다.
 * - `null` 및 타입 불일치 요소는 결과에서 제거됩니다.
 * - 예외 대신 필터링으로 동작합니다.
 *
 * ```kotlin
 * val result = flowOf(1, "x", 2).castNotNull<Int>().toList()
 * // result == [1, 2]
 * ```
 */
inline fun <reified R: Any> Flow<*>.castNotNull(): Flow<R> = mapNotNull { it as? R }

/**
 * non-null Flow를 nullable Flow로 업캐스팅합니다.
 *
 * ## 동작/계약
 * - 요소 값과 순서를 그대로 유지하며 타입만 `T?`로 확장합니다.
 * - 추가 연산/할당 없이 수신 Flow를 그대로 반환합니다.
 *
 * ```kotlin
 * val result: Flow<Int?> = flowOf(1, 2).castNullable()
 * // result는 1, 2를 동일 순서로 방출
 * ```
 */
fun <T: Any> Flow<T>.castNullable(): Flow<T?> = this
