package io.bluetape4k.javatimes.interval


import io.bluetape4k.logging.KLogging
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.Temporal

/**
 * Mutable [TemporalInterval]
 */
class MutableTemporalInterval<T> private constructor(
    start: T,
    end: T,
    override val zoneId: ZoneId,
): AbstractTemporalInterval<T>() where T: Temporal, T: Comparable<T> {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T> invoke(
            start: T,
            end: T,
            zoneId: ZoneId = ZoneOffset.UTC,
        ): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> {
            return MutableTemporalInterval(start, end, zoneId)
        }

        @JvmStatic
        operator fun <T> invoke(
            other: ReadableTemporalInterval<T>,
        ): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> {
            return invoke(other.startInclusive, other.endExclusive, other.zoneId)
        }
    }

    override var startInclusive: T = start
        set(value) {
            if (value > endExclusive) {
                field = endExclusive
                endExclusive = value
            } else {
                field = value
            }
        }

    override var endExclusive: T = end
        set(value) {
            if (value < startInclusive) {
                field = startInclusive
                this.startInclusive = value
            } else {
                field = value
            }
        }

    override fun withStart(newStart: T): MutableTemporalInterval<T> =
        if (newStart < endExclusive) mutableTemporalIntervalOf(newStart, this.endExclusive, zoneId)
        else mutableTemporalIntervalOf(endExclusive, newStart, zoneId)

    override fun withEnd(newEnd: T): ReadableTemporalInterval<T> =
        if (newEnd > startInclusive) mutableTemporalIntervalOf(this.startInclusive, newEnd, zoneId)
        else mutableTemporalIntervalOf(newEnd, this.startInclusive, zoneId)
}
