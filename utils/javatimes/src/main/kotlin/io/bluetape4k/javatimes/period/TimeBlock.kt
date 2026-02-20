package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.MaxDuration
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.time.ZonedDateTime

/**
 * [ITimeBlock]의 기본 구현채
 */
open class TimeBlock(
    start: ZonedDateTime? = MinPeriodTime,
    end: ZonedDateTime? = MaxPeriodTime,
    readonly: Boolean = false,
): TimePeriod(start ?: MinPeriodTime, end ?: MaxPeriodTime, readonly), ITimeBlock {

    constructor(moment: ZonedDateTime?, readonly: Boolean = false): this(moment, moment, readonly)

    companion object: KLogging() {

        @JvmField
        val AnyTime: TimeBlock = TimeBlock(readonly = true)

        @JvmStatic
        operator fun invoke(start: ZonedDateTime, end: ZonedDateTime, readonly: Boolean = false): TimeBlock =
            TimeBlock(start, end, readonly)

        @JvmStatic
        operator fun invoke(src: ITimePeriod, readonly: Boolean = src.readonly): TimeBlock =
            TimeBlock(src.start, src.end, readonly)

        @JvmStatic
        operator fun invoke(moment: ZonedDateTime, readonly: Boolean = false): TimeBlock =
            TimeBlock(moment, moment, readonly)

        @JvmStatic
        operator fun invoke(start: ZonedDateTime, duration: Duration, readonly: Boolean = false): TimeBlock =
            TimeBlock(start, start + duration, readonly)

        @JvmStatic
        operator fun invoke(duration: Duration, end: ZonedDateTime, readonly: Boolean = false): TimeBlock =
            TimeBlock(end - duration, end, readonly)
    }

    private var _duration = super.duration

    override var start: ZonedDateTime
        get() = super.start
        set(value) {
            super.start = value
            refreshDuration()
        }

    override var end: ZonedDateTime
        get() = super.end
        set(value) {
            super.end = value
            refreshDuration()
        }

    override var duration: Duration
        get() = _duration
        set(value) {
            durationFromStart(value)
        }

    override fun setup(newStart: ZonedDateTime, newDuration: Duration) {
        assertMutable()
        assertValidDuration(newDuration)

        start = newStart
        duration = newDuration
    }

    override fun copy(offset: Duration): ITimePeriod {
        if (offset.isZero) {
            return TimeBlock(this)
        }

        val s = if (hasStart) start + offset else start
        val e = if (hasEnd) end + offset else end
        return TimeBlock(s, e, readonly)
    }

    override fun durationFromStart(newDuration: Duration) {
        assertMutable()
        assertValidDuration(newDuration)

        _duration = newDuration
        end = when (_duration) {
            MaxDuration -> MaxPeriodTime
            else        -> start + _duration
        }
    }

    override fun durationFromEnd(newDuration: Duration) {
        assertMutable()
        assertValidDuration(newDuration)

        _duration = newDuration
        start = when (_duration) {
            MaxDuration -> MinPeriodTime
            else        -> end - _duration
        }
    }

    private fun refreshDuration() {
        _duration = when {
            hasPeriod               -> Duration.between(start, end)
            _duration == MaxDuration -> MaxDuration
            else                    -> EmptyDuration
        }
    }

    private fun assertValidDuration(duration: Duration) {
        assert(!duration.isNegative) { "duration은 0 이상의 값을 가져야 합니다." }
    }
}
