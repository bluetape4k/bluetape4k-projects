package io.bluetape4k.javatimes.interval

import io.bluetape4k.ranges.ClosedOpenRange
import java.io.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.Temporal

/**
 * JodaTime 의 `ReadableTemporalInterval` 과 같은 기능을 수행합니다.
 *
 * ```kotlin
 * val interval = temporalIntervalOf(
 *     LocalDate.of(2024, 1, 1),
 *     LocalDate.of(2024, 12, 31)
 * )
 * val contains = interval.contains(LocalDate.of(2024, 6, 15)) // true
 * ```
 */
interface ReadableTemporalInterval<T>:
    ClosedOpenRange<T>,
    Comparable<ClosedRange<T>>,
    Serializable
        where T: Temporal, T: Comparable<T> {

    companion object {
        const val SEPARATOR = "~"

        @JvmStatic
        val EMPTY_INTERVAL =
            temporalIntervalOf<Instant>(
                Instant.ofEpochMilli(0L),
                Instant.ofEpochMilli(0L)
            )
    }

    val zoneId: ZoneId

    /**
     * 두 구간이 인접(끝과 시작이 맞닿음)해 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * val b = temporalIntervalOf(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 31))
     * a.abuts(b) // true
     * ```
     */
    fun abuts(other: ReadableTemporalInterval<T>): Boolean

    /**
     * 두 구간 사이의 간격을 나타내는 구간을 반환합니다. 겹치면 null 을 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1))
     * val b = temporalIntervalOf(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 31))
     * val gap = a.gap(b) // 2024-03-01 ~ 2024-06-01
     * ```
     */
    fun gap(interval: ReadableTemporalInterval<T>): ReadableTemporalInterval<T>?

    /**
     * 두 구간이 겹치는 부분을 반환합니다. 겹치지 않으면 null 을 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * val b = temporalIntervalOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 31))
     * val overlap = a.overlap(b) // 2024-03-01 ~ 2024-06-01
     * ```
     */
    fun overlap(interval: ReadableTemporalInterval<T>): ReadableTemporalInterval<T>?

    /**
     * 두 구간이 겹치는지 여부를 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * val b = temporalIntervalOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 31))
     * a.overlaps(b) // true
     * ```
     */
    fun overlaps(other: ReadableTemporalInterval<T>): Boolean

    /**
     * 특정 시각이 구간 안에 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
     * interval.overlaps(LocalDate.of(2024, 6, 15)) // true
     * ```
     */
    fun overlaps(moment: T): Boolean

    /**
     * 다른 구간이 현재 구간 안에 완전히 포함되는지 여부를 반환합니다.
     *
     * ```kotlin
     * val outer = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
     * val inner = temporalIntervalOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 6, 1))
     * outer.contains(inner) // true
     * ```
     */
    operator fun contains(other: ReadableTemporalInterval<T>): Boolean

    /**
     * epoch milliseconds 값이 구간 안에 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600))
     * interval.contains(1800_000L) // true
     * ```
     */
    operator fun contains(epochMillis: Long): Boolean

    /**
     * 현재 구간이 [other] 구간보다 이전인지 여부를 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * val b = temporalIntervalOf(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31))
     * a.isBefore(b) // true
     * ```
     */
    fun isBefore(other: ReadableTemporalInterval<T>): Boolean

    /**
     * 현재 구간이 [moment] 시각보다 이전인지 여부를 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * interval.isBefore(LocalDate.of(2024, 12, 31)) // true
     * ```
     */
    fun isBefore(moment: T): Boolean

    /**
     * 현재 구간이 [other] 구간보다 이후인지 여부를 반환합니다.
     *
     * ```kotlin
     * val a = temporalIntervalOf(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31))
     * val b = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * a.isAfter(b) // true
     * ```
     */
    fun isAfter(other: ReadableTemporalInterval<T>): Boolean

    /**
     * 현재 구간이 [moment] 시각보다 이후인지 여부를 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31))
     * interval.isAfter(LocalDate.of(2024, 1, 1)) // true
     * ```
     */
    fun isAfter(moment: T): Boolean

    /**
     * 시작 시각을 [newStart]로 변경한 새로운 구간을 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 31))
     * val shifted = interval.withStart(LocalDate.of(2024, 1, 1))
     * // shifted: 2024-01-01 ~ 2024-12-31
     * ```
     */
    fun withStart(newStart: T): ReadableTemporalInterval<T>

    /**
     * 종료 시각을 [newEnd]로 변경한 새로운 구간을 반환합니다.
     *
     * ```kotlin
     * val interval = temporalIntervalOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1))
     * val extended = interval.withEnd(LocalDate.of(2024, 12, 31))
     * // extended: 2024-01-01 ~ 2024-12-31
     * ```
     */
    fun withEnd(newEnd: T): ReadableTemporalInterval<T>
}
