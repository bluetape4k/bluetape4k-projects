package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.PeriodRelation
import io.bluetape4k.javatimes.period.TimePeriodContainer
import io.bluetape4k.javatimes.period.hasInsideWith
import io.bluetape4k.javatimes.period.intersectWith
import io.bluetape4k.javatimes.period.overlapWith
import io.bluetape4k.javatimes.period.relationWith
import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Collections
import java.util.Comparator
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * [ITimeLineMomentCollection]의 기본 구현체입니다.
 *
 * Moment list는 immutable snapshot으로 노출하며, [ITimePeriod] 기반 [add], [addAll], [remove]만
 * timeline 불변식을 유지하면서 내부 상태를 변경합니다. `MutableList` mutation은 기존 ABI를
 * 위해 남아 있지만 상태 변경이나 callback 호출 전에 [UnsupportedOperationException]을 던집니다.
 *
 * Protected constructor와 [addPeriod], [removePeriod]를 사용하는 subclass도 전체 endpoint pair를
 * 원자적으로 추가·제거하는 동일한 계약을 따릅니다.
 *
 * Java serialization은 같은 라이브러리 버전의 round-trip을 지원합니다. 내부 field layout이
 * 변경됐으므로 이전 버전에서 직렬화한 cache/stream은 업그레이드 시 폐기하고 다시 생성해야 합니다.
 *
 * ```kotlin
 * val now = ZonedDateTime.now()
 * val collection = TimeLineMomentCollection()
 * val period = TimeRange(now, now.plusDays(1))
 * collection.add(period)
 * collection.find(now)?.startCount  // 1
 * collection.minOrNull()?.moment    // now
 * collection.maxOrNull()?.moment    // now + 1d
 * ```
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
open class TimeLineMomentCollection private constructor(
    private val storage: TimeLineMomentCollectionStorage,
): ITimeLineMomentCollection, MutableList<ITimeLineMoment> by storage.guarded {

    protected constructor(moments: MutableList<ITimeLineMoment>): this(TimeLineMomentCollectionStorage(moments))

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(moments: MutableList<ITimeLineMoment> = mutableListOf()): TimeLineMomentCollection {
            return TimeLineMomentCollection(moments)
        }
    }

    override fun minOrNull(): ITimeLineMoment? = storage.mutable.minOrNull()

    override fun maxOrNull(): ITimeLineMoment? = storage.mutable.maxOrNull()

    override fun add(period: ITimePeriod) {
        storage.addAll(listOf(period))
    }

    override fun addAll(periods: Collection<ITimePeriod>) {
        storage.addAll(periods)
    }

    override fun remove(period: ITimePeriod) {
        storage.remove(period)
    }

    override fun find(moment: ZonedDateTime): ITimeLineMoment? =
        storage.mutable.find { it.moment == moment }

    override fun contains(moment: ZonedDateTime): Boolean =
        storage.mutable.any { it.moment == moment }

    override fun contains(element: ITimeLineMoment): Boolean =
        storage.indexOf(element) >= 0

    override fun containsAll(elements: Collection<ITimeLineMoment>): Boolean =
        (elements as Collection<*>).all { it is ITimeLineMoment && contains(it) }

    override fun indexOf(element: ITimeLineMoment): Int =
        storage.indexOf(element)

    override fun lastIndexOf(element: ITimeLineMoment): Int =
        storage.lastIndexOf(element)

    override fun removeIf(filter: Predicate<in ITimeLineMoment>): Boolean {
        throw unsupportedMutation()
    }

    override fun replaceAll(operator: UnaryOperator<ITimeLineMoment>) {
        throw unsupportedMutation()
    }

    override fun sort(c: Comparator<in ITimeLineMoment>?) {
        throw unsupportedMutation()
    }

    protected fun addPeriod(moment: ZonedDateTime, period: ITimePeriod) {
        require(moment == period.start || moment == period.end) {
            "Period는 요청한 timeline moment에서 시작하거나 끝나야 합니다. moment=$moment, period=$period"
        }
        storage.addAll(listOf(period))
    }

    protected fun removePeriod(moment: ZonedDateTime, period: ITimePeriod) {
        require(moment == period.start || moment == period.end) {
            "Period는 요청한 timeline moment에서 시작하거나 끝나야 합니다. moment=$moment, period=$period"
        }
        storage.remove(period)
    }

    private fun unsupportedMutation(): UnsupportedOperationException =
        UnsupportedOperationException("TimeLineMomentCollection은 period 기반 mutation만 지원합니다.")
}

private class TimeLineMomentCollectionStorage(
    initialMoments: Collection<ITimeLineMoment>,
): Serializable {
    private var nextOccurrenceId: Long = 0
    private val periods: MutableList<StoredPeriod> = mutableListOf()
    val mutable: MutableList<ITimeLineMoment> = mutableListOf()
    val guarded: MutableList<ITimeLineMoment> = Collections.unmodifiableList(mutable)

    init {
        periods.addAll(validateAndSnapshot(initialMoments))
        rebuildMoments()
    }

    fun addAll(source: Collection<ITimePeriod>) {
        val additions = source.map(::capture)
        val uniqueAdditions = mutableListOf<StoredPeriod>()
        val representatives = periods.mapTo(mutableSetOf()) { it.representative }
        additions.forEach { candidate ->
            if (representatives.add(candidate.representative)) {
                uniqueAdditions += store(candidate)
            }
        }
        if (uniqueAdditions.isNotEmpty()) {
            periods.addAll(uniqueAdditions)
            rebuildMoments()
        }
    }

    fun remove(period: ITimePeriod) {
        val index = periods.indexOfFirst { period == it.representative }
        if (index >= 0) {
            periods.removeAt(index)
            rebuildMoments()
        }
    }

    fun indexOf(moment: ITimeLineMoment): Int =
        mutable.indexOfFirst { it.moment == moment.moment }

    fun lastIndexOf(moment: ITimeLineMoment): Int =
        mutable.indexOfLast { it.moment == moment.moment }

    private fun validateAndSnapshot(initialMoments: Collection<ITimeLineMoment>): List<StoredPeriod> {
        require(initialMoments.distinctBy { it.moment }.size == initialMoments.size) {
            "Timeline moment는 같은 시각에 하나만 존재해야 합니다."
        }

        val groups = linkedMapOf<ITimePeriod, InitialPeriodGroup>()
        initialMoments.forEach { moment ->
            require(moment.periods.isNotEmpty()) {
                "Timeline moment는 하나 이상의 period를 가져야 합니다. moment=${moment.moment}"
            }
            moment.periods.forEach { period ->
                val captured = capture(period)
                require(moment.moment == captured.bounds.start || moment.moment == captured.bounds.end) {
                    "Timeline moment의 period는 해당 시각에서 시작하거나 끝나야 합니다. moment=${moment.moment}"
                }
                val group = groups.getOrPut(captured.representative) { InitialPeriodGroup(captured) }
                require(group.captured.bounds == captured.bounds) {
                    "동일한 period는 모든 moment에서 같은 endpoint를 가져야 합니다."
                }
                group.endpointCounts[moment.moment] = group.endpointCounts.getOrDefault(moment.moment, 0) + 1
            }
        }

        groups.values.forEach { group ->
            val bounds = group.captured.bounds
            val counts = group.endpointCounts
            require(counts.size == 2 && counts[bounds.start] == 1 && counts[bounds.end] == 1) {
                "Period는 시작과 종료 moment에 정확히 한 번씩 연결되어야 합니다. period=$bounds"
            }
        }
        return groups.values.map { store(it.captured) }
    }

    private fun capture(period: ITimePeriod): CapturedPeriod {
        val start = period.start
        val end = period.end
        require(start < end) { "Timeline period는 start가 end보다 빨라야 합니다. start=$start, end=$end" }
        return CapturedPeriod(period, PeriodBounds(start, end))
    }

    private fun store(captured: CapturedPeriod): StoredPeriod {
        val occurrenceId = nextOccurrenceId++
        return StoredPeriod(
            captured.representative,
            ImmutableTimePeriodSnapshot(captured.bounds.start, captured.bounds.end, occurrenceId),
        )
    }

    private fun rebuildMoments() {
        val buckets = sortedMapOf<ZonedDateTime, MutableList<StoredPeriod>>()
        periods.forEach { period ->
            buckets.getOrPut(period.snapshot.start) { mutableListOf() }.add(period)
            buckets.getOrPut(period.snapshot.end) { mutableListOf() }.add(period)
        }

        val rebuilt = buckets.map { (moment, linkedPeriods) ->
            ImmutableTimeLineMomentSnapshot(moment, linkedPeriods)
        }
        var balance = 0L
        rebuilt.forEach { moment ->
            balance += moment.startCount
            balance -= moment.endCount
            check(balance >= 0) { "Timeline balance는 음수가 될 수 없습니다. moment=${moment.moment}, balance=$balance" }
        }
        check(balance == 0L) { "Timeline balance는 0으로 끝나야 합니다. balance=$balance" }

        mutable.clear()
        mutable.addAll(rebuilt)
    }
}

private data class PeriodBounds(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
): Serializable

private data class CapturedPeriod(
    val representative: ITimePeriod,
    val bounds: PeriodBounds,
)

private class StoredPeriod(
    val representative: ITimePeriod,
    val snapshot: ImmutableTimePeriodSnapshot,
): Serializable

private class InitialPeriodGroup(
    val captured: CapturedPeriod,
) {
    val endpointCounts: MutableMap<ZonedDateTime, Int> = mutableMapOf()
}

private class ImmutableTimePeriodSnapshot(
    override val start: ZonedDateTime,
    override val end: ZonedDateTime,
    private val occurrenceId: Long,
): AbstractValueObject(), ITimePeriod {

    private val bounds: PeriodBounds = PeriodBounds(start, end)

    override val readonly: Boolean = true

    override fun setup(newStart: ZonedDateTime?, newEnd: ZonedDateTime?) {
        throw unsupportedMutation()
    }

    override fun copy(offset: Duration): ITimePeriod {
        val shiftedStart = if (hasStart) start + offset else start
        val shiftedEnd = if (hasEnd) end + offset else end
        return ImmutableTimePeriodSnapshot(shiftedStart, shiftedEnd, occurrenceId)
    }

    override fun move(offset: Duration) {
        throw unsupportedMutation()
    }

    override fun isSamePeriod(other: ITimePeriod?): Boolean =
        other != null && start == other.start && end == other.end

    override fun reset() {
        throw unsupportedMutation()
    }

    override fun compareTo(other: ITimePeriod): Int =
        compareValuesBy(this, other, ITimePeriod::start, ITimePeriod::end)

    override fun equalProperties(other: Any): Boolean =
        other is ImmutableTimePeriodSnapshot && occurrenceId == other.occurrenceId && bounds == other.bounds

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = 31 * bounds.hashCode() + occurrenceId.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("start", start)
            .add("end", end)
            .add("readonly", readonly)

    private fun unsupportedMutation(): UnsupportedOperationException =
        UnsupportedOperationException("Timeline period snapshot은 변경할 수 없습니다.")
}

private class ImmutableTimeLineMomentSnapshot(
    override val moment: ZonedDateTime,
    linkedPeriods: Collection<StoredPeriod>,
): AbstractValueObject(), ITimeLineMoment {

    override val periods: ITimePeriodCollection = ImmutableTimePeriodCollection(linkedPeriods)

    override val startCount: Long = periods.count { it.start == moment }.toLong()

    override val endCount: Long = periods.count { it.end == moment }.toLong()

    override fun compareTo(other: ITimeLineMoment): Int =
        moment.compareTo(other.moment)

    override fun equalProperties(other: Any): Boolean =
        other is ITimeLineMoment && moment == other.moment

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = moment.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("moment", moment)
            .add("startCount", startCount)
            .add("endCount", endCount)
            .add("periods", periods)
}

private class ImmutableTimePeriodCollection private constructor(
    private val storage: ImmutableTimePeriodCollectionStorage,
): TimePeriodContainer(storage.guarded), ITimePeriodCollection {

    constructor(periods: Collection<StoredPeriod>): this(ImmutableTimePeriodCollectionStorage(periods))

    override val readonly: Boolean = true

    override fun contains(element: ITimePeriod): Boolean =
        storage.entries.any { element == it.representative || element == it.snapshot }

    override fun containsPeriod(target: ITimePeriod): Boolean =
        contains(target)

    override fun containsAll(elements: Collection<ITimePeriod>): Boolean =
        (elements as Collection<*>).all { it is ITimePeriod && contains(it) }

    override fun indexOf(element: ITimePeriod): Int =
        storage.entries.indexOfFirst { element == it.representative || element == it.snapshot }

    override fun lastIndexOf(element: ITimePeriod): Int =
        storage.entries.indexOfLast { element == it.representative || element == it.snapshot }

    override fun add(element: ITimePeriod): Boolean {
        throw unsupportedMutation()
    }

    override fun add(index: Int, element: ITimePeriod) {
        throw unsupportedMutation()
    }

    override fun addAll(elements: Collection<ITimePeriod>): Boolean {
        throw unsupportedMutation()
    }

    override fun addAll(index: Int, elements: Collection<ITimePeriod>): Boolean {
        throw unsupportedMutation()
    }

    override fun set(index: Int, element: ITimePeriod): ITimePeriod {
        throw unsupportedMutation()
    }

    override fun remove(element: ITimePeriod): Boolean {
        throw unsupportedMutation()
    }

    override fun removeAt(index: Int): ITimePeriod {
        throw unsupportedMutation()
    }

    override fun removeAll(elements: Collection<ITimePeriod>): Boolean {
        throw unsupportedMutation()
    }

    override fun retainAll(elements: Collection<ITimePeriod>): Boolean {
        throw unsupportedMutation()
    }

    override fun clear() {
        throw unsupportedMutation()
    }

    override fun move(offset: Duration) {
        throw unsupportedMutation()
    }

    override fun reset() {
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

    override fun hasInsidePeriods(that: ITimePeriod): Boolean =
        periods.any { it.hasInsideWith(that) }

    override fun hasOverlapPeriods(that: ITimePeriod): Boolean =
        periods.any { it.overlapWith(that) }

    override fun hasIntersectionPeriods(moment: ZonedDateTime): Boolean =
        periods.any { it.hasInsideWith(moment) }

    override fun hasIntersectionPeriods(that: ITimePeriod): Boolean =
        periods.any { it.intersectWith(that) }

    override fun insidePeriods(target: ITimePeriod): List<ITimePeriod> =
        periods.filter { it.hasInsideWith(target) }

    override fun overlapPeriods(target: ITimePeriod): List<ITimePeriod> =
        periods.filter { it.overlapWith(target) }

    override fun intersectionPeriod(moment: ZonedDateTime): List<ITimePeriod> =
        periods.filter { it.hasInsideWith(moment) }

    override fun intersectionPeriod(target: ITimePeriod): List<ITimePeriod> =
        periods.filter { it.intersectWith(target) }

    override fun relationPeriods(target: ITimePeriod, vararg relations: PeriodRelation): List<ITimePeriod> =
        periods.filter { relations.contains(it.relationWith(target)) }

    private fun unsupportedMutation(): UnsupportedOperationException =
        UnsupportedOperationException("Timeline period snapshot collection은 변경할 수 없습니다.")
}

private class ImmutableTimePeriodCollectionStorage(
    periods: Collection<StoredPeriod>,
): Serializable {
    val entries: List<StoredPeriod> = periods.toList()
    val guarded: MutableList<ITimePeriod> = Collections.unmodifiableList(entries.map { it.snapshot }.toMutableList())
}
