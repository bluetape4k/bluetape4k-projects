package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * [start]부터 [count] 수의 [Int] 값을 emit 하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeOf(1, 5).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
 * ```
 *
 * @param start 시작 Int 값
 * @param count 생성할 요소의 수
 * @return Flow<Int>
 */
@JvmName("flowRangeOfInt")
fun flowRangeOf(start: Int, count: Int): Flow<Int> = (start until start + count).asFlow()

/**
 * [start]부터 [count] 수의 [Long] 값을 emit 하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeOf(1L, 5).toList() shouldBeEqualTo listOf(1L, 2L, 3L, 4L, 5L)
 * ```
 *
 * @param start 시작 Long 값
 * @param count 생성할 요소의 수
 * @return Flow<Long>
 */
@JvmName("flowRangeOfLong")
fun flowRangeOf(start: Long, count: Int): Flow<Long> = (start until start + count).asFlow()

/**
 * [start]부터 [count] 수의 [Char] 값을 emit 하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeInt(1, 3).toList() shouldBeEqualTo listOf(1, 2, 3)
 * ```
 *
 * @param start 시작 Int 값
 * @param count 생성할 요소의 수
 * @return Flow<Int>
 *
 * @see flowRangeOf(start: Int, count: Int)
 */
fun flowRangeInt(start: Int, count: Int): Flow<Int> = (start until start + count).asFlow()

/**
 * [start]부터 [count] 수의 [Long] 값을 emit 하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeLong(1L, 3).toList() shouldBeEqualTo listOf(1L, 2L, 3L)
 * ```
 *
 * @param start 시작 Long 값
 * @param count 생성할 요소의 수
 * @return Flow<Long>
 *
 * @see flowRangeOf(start: Long, count: Int)
 */
fun flowRangeLong(start: Long, count: Int): Flow<Long> = (start until start + count).asFlow()

/**
 * [CharProgression]을 [Flow]로 변환합니다.
 *
 * ```
 * ('a'..'c').asFlow().toList() shouldBeEqualTo listOf('a', 'b', 'c')
 * ```
 */
fun CharProgression.asFlow(): Flow<Char> = flow { this@asFlow.asSequence().forEach { emit(it) } }

/**
 * [IntProgression]을 [Flow]로 변환합니다.
 *
 * ```
 * (1..3).asFlow().toList() shouldBeEqualTo listOf(1, 2, 3)
 * ```
 */
fun IntProgression.asFlow(): Flow<Int> = flow { this@asFlow.asSequence().forEach { emit(it) } }

/**
 * [LongProgression]을 [Flow]로 변환합니다.
 *
 * ```
 * (1L..3L).asFlow().toList() shouldBeEqualTo listOf(1L, 2L, 3L)
 * ```
 */
fun LongProgression.asFlow(): Flow<Long> = flow { this@asFlow.asSequence().forEach { emit(it) } }
