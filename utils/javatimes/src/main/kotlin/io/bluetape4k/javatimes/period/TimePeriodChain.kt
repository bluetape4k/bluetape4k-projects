package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.durationOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Collections
import java.util.Comparator
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * [ITimePeriodChain]의 기본 구현체입니다.
 *
 * 체인의 기간은 [add]와 [addAll]로 끝에만 추가할 수 있습니다. `MutableList` 호환성을 위해
 * 노출되는 교체·삭제·정렬 API와 [periods] view의 mutation은 상태를 바꾸기 전에
 * [UnsupportedOperationException]을 던집니다.
 * [periods]를 재정의하는 subclass는 같은 mutation과 contiguity 계약을 직접 보존해야 합니다.
 *
 * ```kotlin
 * val now = ZonedDateTime.now()
 * val chain = TimePeriodChain()
 * chain.add(TimeBlock(now, Duration.ofHours(2)))
 * chain.add(TimeBlock(now.plusHours(2), Duration.ofHours(3)))
 * chain.headOrNull()?.start // now
 * chain.lastOrNull()?.end   // now + 5h
 * chain.size // 2
 * ```
 */
open class TimePeriodChain private constructor(
    private val storage: TimePeriodChainStorage,
): TimePeriodContainer(storage.guarded), ITimePeriodChain {

    constructor(): this(TimePeriodChainStorage())

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
        throw unsupportedMutation()
    }

    override fun add(element: ITimePeriod): Boolean {
        if (containsPeriod(element)) {
            return false
        }
        this.lastOrNull()?.let { last ->
            assertSpaceAfter(last.end, element.duration)
            element.setup(last.end, last.end + element.duration)
        }
        log.trace { "Add element to period chain. element=$element" }
        return storage.mutable.add(element)
    }

    override fun addAll(elements: Collection<ITimePeriod>): Boolean {
        if (elements === this || elements === periods) {
            return false
        }
        return elements.toList().map(::add).any()
    }

    override fun add(index: Int, element: ITimePeriod) {
        throw unsupportedMutation()
    }

    override fun remove(element: ITimePeriod): Boolean {
        throw unsupportedMutation()
    }

    override fun removeIf(filter: Predicate<in ITimePeriod>): Boolean {
        throw unsupportedMutation()
    }

    override fun replaceAll(operator: UnaryOperator<ITimePeriod>) {
        throw unsupportedMutation()
    }

    override fun sort(c: Comparator<in ITimePeriod>?) {
        throw unsupportedMutation()
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

    private fun unsupportedMutation(): UnsupportedOperationException =
        UnsupportedOperationException("TimePeriodChain은 끝에 기간을 추가하는 mutation만 지원합니다.")
}

private class TimePeriodChainStorage {
    val mutable: MutableList<ITimePeriod> = mutableListOf()
    val guarded: MutableList<ITimePeriod> = Collections.unmodifiableList(mutable)
}
