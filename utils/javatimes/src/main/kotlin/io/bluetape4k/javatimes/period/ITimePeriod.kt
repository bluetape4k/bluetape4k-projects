package io.bluetape4k.javatimes.period

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Time period 를 나타내는 최상위 인터페이스
 *
 * ```kotlin
 * val period: ITimePeriod = TimeRange(
 *     ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
 *     ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)
 * )
 * period.hasPeriod  // true
 * period.duration   // Duration.between(start, end)
 * period.isMoment   // false
 * ```
 */
interface ITimePeriod: ValueObject, Comparable<ITimePeriod> {

    /**
     * 기간의 시작 시각
     */
    val start: ZonedDateTime

    /**
     * 기간의 완료 시각
     */
    val end: ZonedDateTime

    /**
     * 읽기 전용 여부
     */
    val readonly: Boolean

    /**
     * [start] ~ [end] 사이의 기간
     */
    val duration: Duration
        get() = when {
            hasPeriod -> Duration.between(start, end)
            else      -> EmptyDuration
        }

    /**
     * 시작 시각이 설정되어 있는지 여부
     */
    val hasStart: Boolean
        get() = start != MinPeriodTime

    /**
     * 완료 시각이 설정되어 있는지 여부
     */
    val hasEnd: Boolean
        get() = end != MaxPeriodTime

    /**
     * 시작 시각과 완료 시각이 설정되어 기간이 있는지 여부
     */
    val hasPeriod: Boolean
        get() = hasStart && hasEnd

    /**
     * 시작 시각과 완료 시각이 같아 순간의 기간을 나타내는지 여부
     */
    val isMoment: Boolean
        get() = start == end

    /**
     * 시작 시각, 완료 시각 모두 설정하지 않아, 기간이 없는 상태인지 여부
     */
    val isAnyTime: Boolean
        get() = !hasStart && !hasEnd

    /**
     * [newStart], [newEnd]로 기간을 다시 설정합니다.
     *
     * ```kotlin
     * val period = TimeBlock()
     * period.setup(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
     *              ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC))
     * ```
     */
    fun setup(newStart: ZonedDateTime? = MinPeriodTime, newEnd: ZonedDateTime? = MaxPeriodTime)

    /**
     * 현 기간에서 [offset] 만큼 이동한 새로운 기간을 생성합니다.
     *
     * ```kotlin
     * val period = TimeRange(ZonedDateTime.now(), ZonedDateTime.now().plusDays(7))
     * val shifted = period.copy(Duration.ofDays(30)) // 30일 뒤로 이동한 새 기간
     * ```
     */
    fun copy(offset: Duration = Duration.ZERO): ITimePeriod

    /**
     * 현 기간에서 [offset] 만큼 이동합니다.
     *
     * ```kotlin
     * val period = TimeBlock(ZonedDateTime.now(), ZonedDateTime.now().plusDays(7))
     * period.move(Duration.ofDays(1)) // 1일 앞으로 이동
     * ```
     */
    fun move(offset: Duration = Duration.ZERO)

    /**
     * 현 기간이 [other]과 같은지 여부
     *
     * ```kotlin
     * val a = TimeRange(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
     *                   ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC))
     * val b = TimeRange(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
     *                   ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC))
     * a.isSamePeriod(b) // true
     * ```
     */
    fun isSamePeriod(other: ITimePeriod?): Boolean

    /**
     * 기간을 초기화합니다. (AnyTime 상태로 리셋)
     *
     * ```kotlin
     * val period = TimeBlock(ZonedDateTime.now(), ZonedDateTime.now().plusDays(7))
     * period.reset()
     * period.isAnyTime // true
     * ```
     */
    fun reset()
}
