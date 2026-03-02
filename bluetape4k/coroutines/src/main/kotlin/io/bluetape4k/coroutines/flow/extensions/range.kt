package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * `start`부터 `count`개 Int를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `start until (start + count)` 구간을 사용합니다.
 * - `count <= 0`이면 빈 Flow를 반환합니다.
 * - 수신 객체를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(0, 3).toList()
 * // out == [0, 1, 2]
 * ```
 * @param start 시작 값입니다.
 * @param count 방출할 요소 개수입니다.
 */
@JvmName("flowRangeOfInt")
fun flowRangeOf(start: Int, count: Int): Flow<Int> = (start until start + count).asFlow()

/**
 * `start`부터 `count`개 Long을 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `start until (start + count)` 구간을 사용합니다.
 * - `count <= 0`이면 빈 Flow를 반환합니다.
 * - 수신 객체를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(0L, 3).toList()
 * // out == [0L, 1L, 2L]
 * ```
 * @param start 시작 값입니다.
 * @param count 방출할 요소 개수입니다.
 */
@JvmName("flowRangeOfLong")
fun flowRangeOf(start: Long, count: Int): Flow<Long> = (start until start + count).asFlow()

/**
 * [flowRangeOf] Int 오버로드의 별칭입니다.
 *
 * ## 동작/계약
 * - 동작은 [flowRangeOf](`Int`, `Int`)와 동일합니다.
 * - `count <= 0`이면 빈 Flow를 반환합니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeInt(0, 3).toList()
 * // out == [0, 1, 2]
 * ```
 * @param start 시작 값입니다.
 * @param count 방출할 요소 개수입니다.
 */
fun flowRangeInt(start: Int, count: Int): Flow<Int> = (start until start + count).asFlow()

/**
 * [flowRangeOf] Long 오버로드의 별칭입니다.
 *
 * ## 동작/계약
 * - 동작은 [flowRangeOf](`Long`, `Int`)와 동일합니다.
 * - `count <= 0`이면 빈 Flow를 반환합니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeLong(0L, 3).toList()
 * // out == [0L, 1L, 2L]
 * ```
 * @param start 시작 값입니다.
 * @param count 방출할 요소 개수입니다.
 */
fun flowRangeLong(start: Long, count: Int): Flow<Long> = (start until start + count).asFlow()

/**
 * [CharProgression]을 순차 방출하는 Flow로 변환합니다.
 *
 * ## 동작/계약
 * - progression 순서를 그대로 유지해 값을 방출합니다.
 * - 수신 progression을 변경하지 않고 새 cold Flow를 반환합니다.
 * - progression 크기에 비례해 순회 비용이 발생합니다.
 *
 * ```kotlin
 * val out = ('a'..'c').asFlow().toList()
 * // out == ['a', 'b', 'c']
 * ```
 */
fun CharProgression.asFlow(): Flow<Char> = flow { this@asFlow.asSequence().forEach { emit(it) } }

/**
 * [IntProgression]을 순차 방출하는 Flow로 변환합니다.
 *
 * ## 동작/계약
 * - progression 순서를 그대로 유지해 값을 방출합니다.
 * - 수신 progression을 변경하지 않고 새 cold Flow를 반환합니다.
 * - progression 크기에 비례해 순회 비용이 발생합니다.
 *
 * ```kotlin
 * val out = (0..2).asFlow().toList()
 * // out == [0, 1, 2]
 * ```
 */
fun IntProgression.asFlow(): Flow<Int> = flow { this@asFlow.asSequence().forEach { emit(it) } }

/**
 * [LongProgression]을 순차 방출하는 Flow로 변환합니다.
 *
 * ## 동작/계약
 * - progression 순서를 그대로 유지해 값을 방출합니다.
 * - 수신 progression을 변경하지 않고 새 cold Flow를 반환합니다.
 * - progression 크기에 비례해 순회 비용이 발생합니다.
 *
 * ```kotlin
 * val out = (0L..2L).asFlow().toList()
 * // out == [0L, 1L, 2L]
 * ```
 */
fun LongProgression.asFlow(): Flow<Long> = flow { this@asFlow.asSequence().forEach { emit(it) } }
