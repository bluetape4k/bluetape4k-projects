package io.bluetape4k.javatimes.period

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.max
import io.bluetape4k.javatimes.min
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.assertGe
import io.bluetape4k.support.assertLe
import io.bluetape4k.support.hashOf
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 기본 [TimePeriod]
 */
open class TimePeriod(
    private var _start: ZonedDateTime = MinPeriodTime,
    private var _end: ZonedDateTime = MaxPeriodTime,
    override val readonly: Boolean = false,
): AbstractValueObject(), ITimePeriod {

    companion object: KLogging() {
        val AnyTime: TimePeriod = TimePeriod(readonly = true)
    }

    constructor(src: ITimePeriod, readonly: Boolean = src.readonly): this(src.start, src.end, readonly)

    constructor(start: ZonedDateTime, duration: Duration, readonly: Boolean = false): this(
        start,
        start + duration,
        readonly
    )

    init {
        val min = _start min _end
        val max = _start max _end

        _start = min ?: MinPeriodTime
        _end = max ?: MaxPeriodTime
    }

    override var start: ZonedDateTime
        get() = _start
        set(value) {
            assertMutable()
            value.assertLe(end, "start")
            _start = value
        }

    override var end: ZonedDateTime
        get() = _end
        set(value) {
            assertMutable()
            value.assertGe(start, "end")
            _end = value
        }

    override fun setup(newStart: ZonedDateTime?, newEnd: ZonedDateTime?) {
        val start1 = newStart ?: MinPeriodTime
        val end1 = newEnd ?: MaxPeriodTime
        _start = (start1 min end1)!!
        _end = (start1 max end1)!!
    }

    override fun copy(offset: Duration): ITimePeriod {
        if (offset.isZero) {
            return TimePeriod(this)
        }
        val newStart = if (hasStart) start + offset else start
        val newEnd = if (hasEnd) end + offset else end
        return TimePeriod(newStart, newEnd, readonly)
    }

    override fun move(offset: Duration) {
        if (offset.isZero) {
            return
        }

        assertMutable()

        when {
            offset.isPositive -> {
                if (hasEnd) end += offset
                if (hasStart) start += offset
            }

            else              -> {
                if (hasStart) start += offset
                if (hasEnd) end += offset
            }
        }
    }

    override fun isSamePeriod(other: ITimePeriod?): Boolean {
        return other != null && start == other.start && end == other.end
    }

    override fun reset() {
        assertMutable()
        start = MinPeriodTime
        end = MaxPeriodTime
    }

    fun assertMutable() {
        check(!readonly) { "This instance is readonly!" }
    }

    override fun compareTo(other: ITimePeriod): Int {
        var comparison = start.compareTo(other.start)
        if (comparison == 0) {
            comparison = end.compareTo(other.end)
        }
        return comparison
    }

    override fun equalProperties(other: Any): Boolean {
        return other is ITimePeriod &&
                start == other.start &&
                end == other.end &&
                readonly == other.readonly
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return hashOf(start, end, readonly)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("start", start)
            .add("end", end)
            .add("readonly", readonly)
    }
}
