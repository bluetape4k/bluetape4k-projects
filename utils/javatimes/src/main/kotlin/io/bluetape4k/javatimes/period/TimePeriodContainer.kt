package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxDuration
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.durationOf
import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.time.ZonedDateTime

/**
 * [ITimePeriodContainer]의 기본 구현체
 */
open class TimePeriodContainer(
    override val periods: MutableList<ITimePeriod> = mutableListOf(),
): TimePeriod(), ITimePeriodContainer, MutableList<ITimePeriod> by periods {

    companion object: KLogging() {
        operator fun invoke(element: ITimePeriod, vararg elements: ITimePeriod): TimePeriodContainer {
            return TimePeriodContainer().apply {
                add(element)
                addAll(elements)
            }
        }

        operator fun invoke(c: Collection<ITimePeriod>): TimePeriodContainer {
            return TimePeriodContainer().apply {
                addAll(c)
            }
        }
    }

    override var start: ZonedDateTime
        get() = if (isEmpty()) MinPeriodTime else periods.minOfOrNull { it.start } ?: MinPeriodTime
        set(value) {
            if (!isEmpty()) move(durationOf(start, value))
        }

    override var end: ZonedDateTime
        get() = if (isEmpty()) MaxPeriodTime else periods.maxOfOrNull { it.end } ?: MaxPeriodTime
        set(value) {
            if (!isEmpty()) move(durationOf(end, value))
        }

    override val duration: Duration
        get() = if (hasPeriod) durationOf(start, end) else MaxDuration

    override val readonly: Boolean = false

    override fun setup(newStart: ZonedDateTime?, newEnd: ZonedDateTime?) {
        throw UnsupportedOperationException("setup is not supported")
    }

    override fun copy(offset: Duration): TimePeriodContainer {
        throw UnsupportedOperationException("copy is not supported")
    }

    override fun add(element: ITimePeriod): Boolean = when (element) {
        is ITimePeriodContainer -> addAll(element)
        else -> if (containsPeriod(element)) false else periods.add(element)
    }

    override fun add(index: Int, element: ITimePeriod) {
        if (element is ITimePeriodContainer) {
            addAll(index, element)
        } else if (!containsPeriod(element)) {
            periods.add(index, element)
        }
    }

    override fun addAll(elements: Collection<ITimePeriod>): Boolean {
        return if (elements.isNotEmpty()) {
            elements.map { add(it) }.any()
        } else false
    }

    override fun addAll(index: Int, elements: Collection<ITimePeriod>): Boolean {
        return if (elements.isNotEmpty()) {
            elements.mapIndexed { i, element -> add(index + i, element) }.any()
        } else false
    }

    override fun move(offset: Duration) {
        if (!offset.isZero) {
            periods.forEach { it.move(offset) }
        }
    }

    override fun reset() {
        clear()
    }

    override fun toString(): String =
        periods.joinToString(separator = ",")
}
