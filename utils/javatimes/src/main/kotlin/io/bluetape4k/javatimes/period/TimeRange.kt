package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.time.ZonedDateTime

/**
 * [ITimeRange]의 기본 구현체
 */
open class TimeRange(
    start: ZonedDateTime? = MinPeriodTime,
    end: ZonedDateTime? = MaxPeriodTime,
    readonly: Boolean = false,
): TimePeriod(start ?: MinPeriodTime, end ?: MaxPeriodTime, readonly), ITimeRange {

    constructor(moment: ZonedDateTime, readonly: Boolean = false): this(moment, moment, readonly)

    companion object: KLogging() {
        @JvmField
        val AnyTime: TimeRange = TimeRange(readonly = true)

        @JvmStatic
        operator fun invoke(src: ITimePeriod, readonly: Boolean = src.readonly): TimeRange =
            TimeRange(src.start, src.end, readonly)

        @JvmStatic
        operator fun invoke(moment: ZonedDateTime, readonly: Boolean = false): TimeRange =
            TimeRange(moment, moment, readonly)

        @JvmStatic
        operator fun invoke(start: ZonedDateTime?, end: ZonedDateTime?, readonly: Boolean = false): TimeRange =
            TimeRange(start ?: MinPeriodTime, end ?: MaxPeriodTime, readonly)

        @JvmStatic
        operator fun invoke(start: ZonedDateTime, duration: Duration, readonly: Boolean = false): TimeRange =
            TimeRange(start, start + duration, readonly)

        @JvmStatic
        operator fun invoke(duration: Duration, end: ZonedDateTime, readonly: Boolean = false): TimeRange =
            TimeRange(end - duration, end, readonly)

    }

    override fun copy(offset: Duration): ITimePeriod {
        if (offset.isZero) {
            return TimePeriod(this, readonly)
        }
        val s = if (hasStart) start + offset else start
        val e = if (hasEnd) end + offset else end
        return TimeRange(s, e, readonly)
    }

    override fun expandStartTo(moment: ZonedDateTime) {
        if (start > moment) {
            start = moment
        }
    }

    override fun expandEndTo(moment: ZonedDateTime) {
        if (end < moment) {
            end = moment
        }
    }

    override fun shrinkStartTo(moment: ZonedDateTime) {
        if (hasInsideWith(moment) && start < moment) {
            start = moment
        }
    }

    override fun shrinkEndTo(moment: ZonedDateTime) {
        if (hasInsideWith(moment) && moment < end)
            end = moment
    }
}
