package io.bluetape4k.javatimes.period

import java.time.ZonedDateTime

/**
 * 기간의 시작 시각, 완료 시각에 대한 매핑을 수행하는 인터페이스.
 *
 * 시작, 완료 시각의 기간 내 포함 여부를 조절한다
 *
 * ```kotlin
 * val calendar = TimeCalendar.Default
 * val now = ZonedDateTime.now()
 * val mappedStart = calendar.mapStart(now) // startOffset 이 적용된 시작 시각
 * val mappedEnd = calendar.mapEnd(now)     // endOffset 이 적용된 종료 시각
 * ```
 */
interface ITimePeriodMapper {

    /**
     * 시작 시각에 offset 을 적용하여 반환합니다.
     *
     * ```kotlin
     * val calendar = TimeCalendar.Default
     * val start = ZonedDateTime.now()
     * val mapped = calendar.mapStart(start) // start + startOffset
     * ```
     */
    fun mapStart(moment: ZonedDateTime): ZonedDateTime

    /**
     * 종료 시각에 offset 을 적용하여 반환합니다.
     *
     * ```kotlin
     * val calendar = TimeCalendar.Default
     * val end = ZonedDateTime.now()
     * val mapped = calendar.mapEnd(end) // end + endOffset
     * ```
     */
    fun mapEnd(moment: ZonedDateTime): ZonedDateTime

    /**
     * 시작 시각의 offset 을 제거하여 반환합니다.
     *
     * ```kotlin
     * val calendar = TimeCalendar.Default
     * val mapped = calendar.mapStart(ZonedDateTime.now())
     * val original = calendar.unmapStart(mapped) // mapped - startOffset
     * ```
     */
    fun unmapStart(moment: ZonedDateTime): ZonedDateTime

    /**
     * 종료 시각의 offset 을 제거하여 반환합니다.
     *
     * ```kotlin
     * val calendar = TimeCalendar.Default
     * val mapped = calendar.mapEnd(ZonedDateTime.now())
     * val original = calendar.unmapEnd(mapped) // mapped - endOffset
     * ```
     */
    fun unmapEnd(moment: ZonedDateTime): ZonedDateTime

}
