package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeBlock
import io.bluetape4k.javatimes.period.TimePeriod
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Comparator

class TimeLineMomentCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val start = zonedDateTimeOf(2024, 1, 1)
    private val end = zonedDateTimeOf(2024, 1, 10)

    @Test
    fun `empty collection has size zero`() {
        val collection = TimeLineMomentCollection()
        collection.size shouldBeEqualTo 0
    }

    @Test
    fun `add period creates two moments start and end`() {
        val collection = TimeLineMomentCollection()
        val period = TimeRange(start, end)
        collection.add(period)

        collection shouldHaveSize 2
        collection.find(start).shouldNotBeNull()
        collection.find(end).shouldNotBeNull()
    }

    @Test
    fun `find returns moment at the given time`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))

        collection.find(start)?.moment shouldBeEqualTo start
        collection.find(end)?.moment shouldBeEqualTo end
    }

    @Test
    fun `find returns null for non-existent moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.find(start.plusDays(3)).shouldBeNull()
    }

    @Test
    fun `contains returns true for existing moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.contains(start).shouldBeTrue()
    }

    @Test
    fun `minOrNull returns earliest moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.minOrNull()?.moment shouldBeEqualTo start
    }

    @Test
    fun `maxOrNull returns latest moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.maxOrNull()?.moment shouldBeEqualTo end
    }

    @Test
    fun `remove period removes associated moments`() {
        val collection = TimeLineMomentCollection()
        val period = TimeRange(start, end)
        collection.add(period)
        collection.remove(period)

        // After removal, moments with no periods should be removed
        collection.size shouldBeEqualTo 0
    }

    @Test
    fun `addAll from multiple periods creates all moments`() {
        val collection = TimeLineMomentCollection()
        val p1 = TimeRange(start, end)
        val p2 = TimeRange(end, end.plusDays(5))
        collection.addAll(listOf(p1, p2))

        // start, end (shared), end+5d → 3 distinct moment times
        collection.find(start).shouldNotBeNull()
        collection.find(end).shouldNotBeNull()
        collection.find(end.plusDays(5)).shouldNotBeNull()
    }

    @Test
    fun `startCount from moment reflects period count`() {
        val collection = TimeLineMomentCollection()
        val p1 = TimeRange(start, end)
        val p2 = TimeRange(start, end.plusDays(5))
        collection.add(p1)
        collection.add(p2)

        collection.find(start)?.startCount shouldBeEqualTo 2L
    }

    @Test
    fun `same endpoints preserve distinct period equality occurrences`() {
        val range = TimeRange(start, end)
        val readonlyRange = TimeRange(start, end, readonly = true)
        val block = TimeBlock(start, end)
        val collection = TimeLineMomentCollection()

        collection.addAll(listOf(range, readonlyRange, block, range))

        collection.find(start)?.startCount shouldBeEqualTo 3L
        collection.find(end)?.endCount shouldBeEqualTo 3L
        collection.find(start)?.periods?.contains(range).shouldBeTrue()
        collection.find(start)?.periods?.containsPeriod(range).shouldBeTrue()
        collection.find(start)?.periods?.contains(readonlyRange).shouldBeTrue()
        collection.find(start)?.periods?.contains(block).shouldBeTrue()

        collection.remove(readonlyRange)

        collection.find(start)?.startCount shouldBeEqualTo 2L
        collection.find(start)?.periods?.contains(readonlyRange).shouldBeFalse()
        collection.find(start)?.periods?.contains(range).shouldBeTrue()
        collection.find(start)?.periods?.contains(block).shouldBeTrue()
    }

    @Test
    fun `moment contains and indexOf keep timestamp lookup semantics`() {
        val collection = TimeLineMomentCollection().apply { add(TimeRange(start, end)) }
        val equivalentMoment = TimeLineMoment(start)

        collection.contains(equivalentMoment).shouldBeTrue()
        collection.containsAll(listOf(equivalentMoment)).shouldBeTrue()
        collection.indexOf(equivalentMoment) shouldBeEqualTo 0
        collection.lastIndexOf(equivalentMoment) shouldBeEqualTo 0
    }

    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun `Java containsAll returns false for incompatible element types`() {
        val collection = collectionOfTwoMoments()
        val rawMoments = collection as java.util.List<Any?>
        val rawPeriods = collection.first().periods as java.util.List<Any?>

        rawMoments.containsAll(listOf("not-a-moment")).shouldBeFalse()
        rawPeriods.containsAll(listOf("not-a-period")).shouldBeFalse()
    }

    @Test
    fun `raw MutableList mutation paths fail before changing moments`() {
        val mutations = listOf<RawMutationCase>(
            RawMutationCase("add") { it.add(TimeLineMoment(end.plusDays(1))) },
            RawMutationCase("indexed add") { it.add(0, TimeLineMoment(end.plusDays(1))) },
            RawMutationCase("addAll") { it.addAll(listOf(TimeLineMoment(end.plusDays(1)))) },
            RawMutationCase("indexed addAll") { it.addAll(0, listOf(TimeLineMoment(end.plusDays(1)))) },
            RawMutationCase("set") { it[0] = TimeLineMoment(end.plusDays(1)) },
            RawMutationCase("removeAt") { it.removeAt(0) },
            RawMutationCase("remove") { it.remove(it.first()) },
            RawMutationCase("removeAll") { it.removeAll(listOf(it.first())) },
            RawMutationCase("retainAll") { it.retainAll(listOf(it.first())) },
            RawMutationCase("clear") { it.clear() },
            RawMutationCase("iterator remove") { list -> list.iterator().apply { next(); remove() } },
            RawMutationCase("listIterator add") { list -> list.listIterator().add(TimeLineMoment(end.plusDays(1))) },
            RawMutationCase("listIterator set") { list -> list.listIterator().apply { next(); set(TimeLineMoment(end.plusDays(1))) } },
            RawMutationCase("listIterator remove") { list -> list.listIterator().apply { next(); remove() } },
            RawMutationCase("subList clear") { it.subList(0, 1).clear() },
            RawMutationCase("subList add") { it.subList(0, 1).add(TimeLineMoment(end.plusDays(1))) },
            RawMutationCase("subList set") { it.subList(0, 1)[0] = TimeLineMoment(end.plusDays(1)) },
            RawMutationCase("subList removeAt") { it.subList(0, 1).removeAt(0) },
        )

        mutations.forEach { mutation ->
            val collection = collectionOfTwoMoments()
            val before = collection.snapshot()

            try {
                assertFailsWith<UnsupportedOperationException> {
                    mutation.mutate(collection)
                }
            } catch (failure: AssertionError) {
                throw AssertionError("${mutation.name} must be rejected.", failure)
            }

            collection.snapshot() shouldBeEqualTo before
        }
    }

    @Test
    fun `Java default mutation callbacks fail before invocation`() {
        val mutations = listOf<CallbackMutationCase>(
            CallbackMutationCase("removeIf") { list, invoked ->
                list.removeIf {
                    invoked()
                    true
                }
            },
            CallbackMutationCase("replaceAll") { list, invoked ->
                list.replaceAll {
                    invoked()
                    it
                }
            },
            CallbackMutationCase("sort") { list, invoked ->
                list.sortWith(
                    Comparator { _, _ ->
                        invoked()
                        0
                    },
                )
            },
        )

        mutations.forEach { mutation ->
            val collection = collectionOfTwoMoments()
            val before = collection.snapshot()
            var invoked = false

            assertFailsWith<UnsupportedOperationException> {
                mutation.mutate(collection) { invoked = true }
            }

            invoked.shouldBeFalse()
            collection.snapshot() shouldBeEqualTo before
        }
    }

    @Test
    fun `initial moments are copied sorted and isolated from source mutation`() {
        val period = TimeRange(start, end)
        val endMoment = TimeLineMoment(end).apply { periods.add(period) }
        val startMoment = TimeLineMoment(start).apply { periods.add(period) }
        val source = mutableListOf<ITimeLineMoment>(endMoment, startMoment)

        val collection = TimeLineMomentCollection(source)
        source.clear()
        startMoment.periods.clear()
        period.move(1.days())

        collection.map { it.moment } shouldBeEqualTo listOf(start, end)
        collection.first().startCount shouldBeEqualTo 1L
        collection.last().endCount shouldBeEqualTo 1L
    }

    @Test
    fun `invalid initial moments are rejected`() {
        val period = TimeRange(start, end)
        val empty = TimeLineMoment(start)
        val unrelated = TimeLineMoment(start.plusDays(1)).apply { periods.add(period) }
        val duplicated = TimeLineMoment(start).apply { periods.add(period) }
        val missingEnd = TimeLineMoment(start).apply { periods.add(period) }

        listOf(
            listOf(empty),
            listOf(unrelated),
            listOf(duplicated, duplicated),
            listOf(missingEnd),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                TimeLineMomentCollection(invalid.toMutableList())
            }
        }
    }

    @Test
    fun `moment period is rejected without changing collection`() {
        val collection = TimeLineMomentCollection()

        assertFailsWith<IllegalArgumentException> {
            collection.add(TimeRange(start, start))
        }

        collection.isEmpty().shouldBeTrue()
    }

    @Test
    fun `reversed period is rejected without changing collection`() {
        val collection = TimeLineMomentCollection()

        assertFailsWith<IllegalArgumentException> {
            collection.add(ReversedPeriod(start, end))
        }

        collection.isEmpty().shouldBeTrue()
    }

    @Test
    fun `mixed addAll validates all periods before changing collection`() {
        val collection = TimeLineMomentCollection()

        assertFailsWith<IllegalArgumentException> {
            collection.addAll(listOf(TimeRange(start, end), TimeRange(end, end)))
        }

        collection.isEmpty().shouldBeTrue()
    }

    @Test
    fun `exposed moments and periods are immutable snapshots`() {
        val original = TimeRange(start, end)
        val collection = TimeLineMomentCollection().apply { add(original) }
        val before = collection.snapshot()
        val exposedMoment = collection.first()
        val exposedPeriod = exposedMoment.periods.first()

        exposedMoment.periods.contains(exposedPeriod).shouldBeTrue()
        exposedMoment.periods.indexOf(exposedPeriod) shouldBeEqualTo 0
        assertFailsWith<UnsupportedOperationException> { exposedMoment.periods.clear() }
        assertFailsWith<UnsupportedOperationException> { exposedMoment.periods.add(TimeRange(start, end.plusDays(1))) }
        assertFailsWith<UnsupportedOperationException> { exposedMoment.periods.add(exposedPeriod) }
        assertFailsWith<UnsupportedOperationException> { exposedMoment.periods.remove(exposedPeriod) }
        var nestedCallbackInvoked = false
        assertFailsWith<UnsupportedOperationException> {
            exposedMoment.periods.removeIf {
                nestedCallbackInvoked = true
                true
            }
        }
        nestedCallbackInvoked.shouldBeFalse()
        assertFailsWith<UnsupportedOperationException> { exposedPeriod.move(1.days()) }
        assertFailsWith<UnsupportedOperationException> { exposedPeriod.setup(start.plusDays(1), end.plusDays(1)) }
        assertFailsWith<UnsupportedOperationException> { exposedPeriod.reset() }

        original.move(1.days())

        collection.snapshot() shouldBeEqualTo before
    }

    @Test
    fun `nested period collection rejects full MutableList mutation surface`() {
        val mutations = listOf<NestedMutationCase>(
            NestedMutationCase("add") { it.add(TimeRange(start, end.plusDays(1))) },
            NestedMutationCase("indexed add") { it.add(0, TimeRange(start, end.plusDays(1))) },
            NestedMutationCase("addAll") { it.addAll(listOf(TimeRange(start, end.plusDays(1)))) },
            NestedMutationCase("indexed addAll") { it.addAll(0, listOf(TimeRange(start, end.plusDays(1)))) },
            NestedMutationCase("set") { it[0] = TimeRange(start, end.plusDays(1)) },
            NestedMutationCase("remove") { it.remove(it.first()) },
            NestedMutationCase("removeAt") { it.removeAt(0) },
            NestedMutationCase("removeAll") { it.removeAll(listOf(it.first())) },
            NestedMutationCase("retainAll") { it.retainAll(listOf(it.first())) },
            NestedMutationCase("clear") { it.clear() },
            NestedMutationCase("iterator remove") { list -> list.iterator().apply { next(); remove() } },
            NestedMutationCase("listIterator add") { list -> list.listIterator().add(TimeRange(start, end.plusDays(1))) },
            NestedMutationCase("listIterator set") { list -> list.listIterator().apply { next(); set(TimeRange(start, end.plusDays(1))) } },
            NestedMutationCase("listIterator remove") { list -> list.listIterator().apply { next(); remove() } },
            NestedMutationCase("subList clear") { it.subList(0, 1).clear() },
            NestedMutationCase("subList add") { it.subList(0, 1).add(TimeRange(start, end.plusDays(1))) },
            NestedMutationCase("subList set") { it.subList(0, 1)[0] = TimeRange(start, end.plusDays(1)) },
            NestedMutationCase("subList removeAt") { it.subList(0, 1).removeAt(0) },
            NestedMutationCase("removeIf") { it.removeIf { true } },
            NestedMutationCase("replaceAll") { it.replaceAll { period -> period } },
            NestedMutationCase("sort") { it.sortWith(compareByDescending(ITimePeriod::end)) },
        )

        mutations.forEach { mutation ->
            val collection = collectionWithTwoPeriodsAtStart()
            val periods = collection.first().periods
            val before = collection.snapshot()

            try {
                assertFailsWith<UnsupportedOperationException> { mutation.mutate(periods) }
            } catch (failure: AssertionError) {
                throw AssertionError("${mutation.name} must be rejected.", failure)
            }

            collection.snapshot() shouldBeEqualTo before
        }
    }

    @Test
    fun `snapshot copy preserves open period boundaries`() {
        val collection = TimeLineMomentCollection().apply { add(TimePeriod.AnyTime) }

        val copy = collection.first().periods.first().copy(1.days())

        copy.start shouldBeEqualTo MinPeriodTime
        copy.end shouldBeEqualTo MaxPeriodTime
    }

    @Test
    fun `collection remains serializable with immutable snapshots`() {
        val original = TimeLineMomentCollection().apply {
            addAll(
                listOf(
                    TimeRange(start, end),
                    TimeBlock(end, end.plusDays(2)),
                ),
            )
        }

        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(original) }
            output.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as TimeLineMomentCollection
        }

        restored.snapshot() shouldBeEqualTo original.snapshot()
        assertFailsWith<UnsupportedOperationException> { restored.clear() }
        restored.add(TimeRange(end.plusDays(2), end.plusDays(3)))
        restored.map { it.moment } shouldBeEqualTo
            listOf(start, end, end.plusDays(2), end.plusDays(3))
    }

    @Test
    fun `validated constructor input remains compatible with timeline algorithms`() {
        val first = TimeRange(start, end)
        val second = TimeRange(end.minusDays(2), end.plusDays(3))
        val source = TimeLineMomentCollection().apply { addAll(listOf(first, second)) }
        val collection = TimeLineMomentCollection(source.toMutableList())

        val combined = TimeLines.combinePeriods(collection)
        val intersections = TimeLines.intersectPeriods(collection)
        val gaps = TimeLines.calculateGap(collection, TimeRange(start.minusDays(1), end.plusDays(4)))

        combined shouldHaveSize 1
        combined.first().start shouldBeEqualTo start
        combined.first().end shouldBeEqualTo end.plusDays(3)
        intersections shouldHaveSize 1
        intersections.first().start shouldBeEqualTo end.minusDays(2)
        intersections.first().end shouldBeEqualTo end
        gaps shouldHaveSize 2
        gaps.first().start shouldBeEqualTo start.minusDays(1)
        gaps.last().end shouldBeEqualTo end.plusDays(4)
    }

    @Test
    fun `protected constructor and mutation hooks preserve canonical pairs`() {
        val period = TimeRange(start, end)
        val startMoment = TimeLineMoment(start).apply { periods.add(period) }
        val endMoment = TimeLineMoment(end).apply { periods.add(period) }
        val collection = DerivedMomentCollection(mutableListOf(endMoment, startMoment))
        val next = TimeRange(end, end.plusDays(3))

        collection.addThroughProtected(end, next)

        collection.map { it.moment } shouldBeEqualTo
            listOf(start, end, end.plusDays(3))

        collection.removeThroughProtected(start, period)

        collection.map { it.moment } shouldBeEqualTo listOf(end, end.plusDays(3))
        collection.find(end)?.startCount shouldBeEqualTo 1L
        collection.find(end)?.endCount shouldBeEqualTo 0L
    }

    private fun collectionOfTwoMoments(): TimeLineMomentCollection =
        TimeLineMomentCollection().apply {
            add(TimeRange(start, end))
        }

    private fun collectionWithTwoPeriodsAtStart(): TimeLineMomentCollection =
        TimeLineMomentCollection().apply {
            addAll(listOf(TimeRange(start, end), TimeRange(start, end.plusDays(1))))
        }

    private fun TimeLineMomentCollection.snapshot(): List<MomentSnapshot> =
        map { MomentSnapshot(it.moment, it.startCount, it.endCount, it.periods.toList()) }

    private data class RawMutationCase(
        val name: String,
        val mutate: (MutableList<ITimeLineMoment>) -> Unit,
    )

    private data class CallbackMutationCase(
        val name: String,
        val mutate: (MutableList<ITimeLineMoment>, invoked: () -> Unit) -> Unit,
    )

    private data class NestedMutationCase(
        val name: String,
        val mutate: (MutableList<ITimePeriod>) -> Unit,
    )

    private data class MomentSnapshot(
        val moment: java.time.ZonedDateTime,
        val startCount: Long,
        val endCount: Long,
        val periods: List<ITimePeriod>,
    )

    private class ReversedPeriod(
        earlier: java.time.ZonedDateTime,
        later: java.time.ZonedDateTime,
    ): ITimePeriod by TimeRange(earlier, later) {
        override val start: java.time.ZonedDateTime = later
        override val end: java.time.ZonedDateTime = earlier
    }

    private class DerivedMomentCollection(
        moments: MutableList<ITimeLineMoment>,
    ): TimeLineMomentCollection(moments) {
        fun addThroughProtected(moment: java.time.ZonedDateTime, period: ITimePeriod) {
            addPeriod(moment, period)
        }

        fun removeThroughProtected(moment: java.time.ZonedDateTime, period: ITimePeriod) {
            removePeriod(moment, period)
        }
    }
}
