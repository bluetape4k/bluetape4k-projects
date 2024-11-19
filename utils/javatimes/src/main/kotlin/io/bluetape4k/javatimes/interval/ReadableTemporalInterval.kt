package io.bluetape4k.javatimes.interval

import io.bluetape4k.ranges.ClosedOpenRange
import java.io.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.Temporal

/**
 * JodaTime 의 `ReadableTemporalInterval` 과 같은 기능을 수행합니다.
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
            temporalIntervalOf<Instant>(Instant.ofEpochMilli(0L), Instant.ofEpochMilli(0L))
    }

    val zoneId: ZoneId

    fun abuts(other: ReadableTemporalInterval<T>): Boolean
    fun gap(interval: ReadableTemporalInterval<T>): ReadableTemporalInterval<T>?
    fun overlap(interval: ReadableTemporalInterval<T>): ReadableTemporalInterval<T>?

    fun overlaps(other: ReadableTemporalInterval<T>): Boolean
    fun overlaps(moment: T): Boolean

    operator fun contains(other: ReadableTemporalInterval<T>): Boolean
    operator fun contains(epochMillis: Long): Boolean

    fun isBefore(other: ReadableTemporalInterval<T>): Boolean
    fun isBefore(moment: T): Boolean

    fun isAfter(other: ReadableTemporalInterval<T>): Boolean
    fun isAfter(moment: T): Boolean

    fun withStart(newStart: T): ReadableTemporalInterval<T>
    fun withEnd(newEnd: T): ReadableTemporalInterval<T>
}
