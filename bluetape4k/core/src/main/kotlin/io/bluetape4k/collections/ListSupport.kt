package io.bluetape4k.collections

/**
 * IntRange를 List<Int>로 변환합니다.
 *
 * ```
 * val range = 1..5
 * val list = intRangeOf(range) // [1, 2, 3, 4, 5]
 *
 * @param range IntRange
 * @return List<Int>
 */
fun intRangeOf(range: IntRange): List<Int> = range.toList()

/**
 * LongRange를 List<Long>로 변환합니다.
 *
 * ```
 * val range = 1L..5L
 * val list = longRangeOf(range) // [1, 2, 3, 4, 5]
 * ```
 *
 * @param range LongRange
 * @return List<Long>
 */
fun longRangeOf(range: LongRange): List<Long> = range.toList()

/**
 * [start] 값과 [count]로 IntRange를 List<Int>로 변환합니다.
 *
 * ```
 * val list: List<Int> = intRangeOf(1, 3) // [1, 2, 3]
 * ```
 *
 * @param start 시작 값
 * @param count 개수
 * @return List<Int>
 */
fun intRangeOf(start: Int, count: Int): List<Int> = intRangeOf(start..<start + count)

/**
 * 시작 값과 개수로 LongRange를 List<Long>로 변환합니다.
 *
 * ```
 * val list: List<Long> = longRangeOf(1L, 3) // [1, 2, 3]
 * ```
 *
 * @param start 시작 값
 * @param count 개수
 * @return List<Long>
 */
fun longRangeOf(start: Long, count: Int): List<Long> = longRangeOf(start..<start + count)
