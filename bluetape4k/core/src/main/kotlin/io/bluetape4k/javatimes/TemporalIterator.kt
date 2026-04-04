package io.bluetape4k.javatimes

import java.time.temporal.Temporal

/**
 * [Temporal] 타입의 값들에 대한 반복자(iterator)
 *
 * [LocalDate], [LocalDateTime], [ZonedDateTime] 등 [Temporal] 구현체의 범위를 순회할 때
 * 사용하는 기반 클래스입니다. 구현 클래스에서 [nextTemporal]을 오버라이드합니다.
 *
 * ```kotlin
 * // LocalDate 범위를 하루씩 순회하는 구현 예시
 * class LocalDateIterator(
 *     startDate: LocalDate,
 *     endDate: LocalDate,
 * ) : TemporalIterator<LocalDate>() {
 *     private var current = startDate
 *     private val end = endDate
 *
 *     override fun hasNext(): Boolean = !current.isAfter(end)
 *     override fun nextTemporal(): LocalDate {
 *         val result = current
 *         current = current.plusDays(1)
 *         return result
 *     }
 * }
 *
 * val start = LocalDate.of(2024, 3, 1)
 * val end   = LocalDate.of(2024, 3, 3)
 * val iter  = LocalDateIterator(start, end)
 *
 * iter.next() // 2024-03-01
 * iter.next() // 2024-03-02
 * iter.next() // 2024-03-03
 * iter.hasNext() // false
 * ```
 */
abstract class TemporalIterator<out T: Temporal>: Iterator<T> {

    /**
     * 박싱 없이 시퀀스의 다음 값을 반환합니다.
     */
    abstract fun nextTemporal(): T

    /**
     * 반복에서 다음 요소를 반환합니다.
     */
    final override fun next(): T = nextTemporal()
}
