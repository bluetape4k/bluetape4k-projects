package io.bluetape4k.javatimes

import java.util.*

/**
 * [Date]를 열거하는 Iterator 입니다.
 *
 * [Date] 범위를 순회하는 커스텀 이터레이터의 기반 클래스입니다.
 * 구현 클래스에서 [nextDate]를 오버라이드하여 다음 날짜를 반환합니다.
 *
 * ```kotlin
 * // java.sql.Date 범위를 하루씩 순회하는 구현 예시
 * class DailyDateIterator(
 *     startDate: java.sql.Date,
 *     endDate: java.sql.Date,
 * ) : DateIterator<java.sql.Date>() {
 *     private var current = startDate.time
 *     private val end = endDate.time
 *     private val step = 24 * 60 * 60 * 1000L // 하루(ms)
 *
 *     override fun hasNext(): Boolean = current <= end
 *     override fun nextDate(): java.sql.Date {
 *         val result = java.sql.Date(current)
 *         current += step
 *         return result
 *     }
 * }
 *
 * val start = java.sql.Date.valueOf("2024-01-01")
 * val end   = java.sql.Date.valueOf("2024-01-03")
 * val iter  = DailyDateIterator(start, end)
 *
 * iter.next() // 2024-01-01
 * iter.next() // 2024-01-02
 * iter.next() // 2024-01-03
 * ```
 *
 * @param T [Date]의 하위 클래스
 */
abstract class DateIterator<out T: Date>: Iterator<T> {

    /**
     * 다음 [Date]를 반환합니다.
     */
    abstract fun nextDate(): T

    /**
     * 다음 [T] 반환합니다. (Date를 상속받는 클래스)
     */
    final override fun next(): T = nextDate()
}
