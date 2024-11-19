package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.durationOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.time.Duration
import java.time.ZonedDateTime

/**
 * [ITimePeriodChain] 의 기본 구현체
 */
open class TimePeriodChain: TimePeriodContainer(), ITimePeriodChain {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(element: ITimePeriod, vararg elements: ITimePeriod): TimePeriodChain {
            return TimePeriodChain().apply {
                add(element)
                addAll(elements)
            }
        }

        @JvmStatic
        operator fun invoke(c: Collection<ITimePeriod>): TimePeriodChain =
            TimePeriodChain().apply {
                addAll(c)
            }
    }

    override var start: ZonedDateTime
        get() = headOrNull()?.start ?: MinPeriodTime
        set(value) {
            move(durationOf(start, value))
        }

    override var end: ZonedDateTime
        get() = lastOrNull()?.end ?: MaxPeriodTime
        set(value) {
            move(durationOf(end, value))
        }

    override operator fun set(index: Int, element: ITimePeriod): ITimePeriod {
        removeAt(index)
        add(index, element)
        return element
    }

    override fun add(element: ITimePeriod): Boolean {
        this.lastOrNull()?.let { last ->
            assertSpaceAfter(last.end, element.duration)
            element.setup(last.end, last.end + element.duration)
        }
        log.trace { "Add eleemnt to period chain. element=$element" }
        return periods.add(element)
    }

    override fun add(index: Int, element: ITimePeriod) {
        throw UnsupportedOperationException("Chain에는 중간에 삽입 기능은 지원하지 않습니다.")
    }

    override fun remove(element: ITimePeriod): Boolean {
        throw UnsupportedOperationException("Chain에서는 remove 기능을 지원하지 않습니다.")
    }

    override fun assertSpaceBefore(moment: ZonedDateTime, duration: Duration) {
        var hasSpace = moment != MinPeriodTime
        if (hasSpace) {
            hasSpace = duration <= durationOf(MinPeriodTime, moment)
        }
        check(hasSpace) { "duration[$duration] is out of range." }
    }

    override fun assertSpaceAfter(moment: ZonedDateTime, duration: Duration) {
        var hasSpace = moment != MaxPeriodTime
        if (hasSpace) {
            hasSpace = duration <= durationOf(moment, MaxPeriodTime)
        }
        check(hasSpace) { "duration[$duration] is out of range." }
    }
}
