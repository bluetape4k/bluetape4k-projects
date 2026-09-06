package io.bluetape4k.javatimes.period

import io.bluetape4k.SortDirection
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.hours
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.Duration

class TimePeriodChainTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `empty chain has MinPeriodTime start and MaxPeriodTime end`() {
        val chain = TimePeriodChain()
        chain.start shouldBeEqualTo MinPeriodTime
        chain.end shouldBeEqualTo MaxPeriodTime
        chain.size shouldBeEqualTo 0
        chain.isEmpty().shouldBeTrue()
    }

    @Test
    fun `add single element sets start and end correctly`() {
        val chain = TimePeriodChain()
        val block = TimeBlock(now, now + Duration.ofHours(2))
        chain.add(block)

        chain.size shouldBeEqualTo 1
        chain.start shouldBeEqualTo now
        chain.end shouldBeEqualTo now.plusHours(2)
    }

    @Test
    fun `add multiple elements chains them sequentially`() {
        val chain = TimePeriodChain()
        chain.add(TimeBlock(now, now + Duration.ofHours(2)))
        chain.add(TimeBlock(now.plusHours(2), now.plusHours(5)))

        chain.size shouldBeEqualTo 2
        chain.start shouldBeEqualTo now
        chain.end shouldBeEqualTo now.plusHours(5)
    }

    @Test
    fun `invoke with collection creates chain`() {
        val periods = listOf(
            TimeBlock(now, now + 1.hours()),
            TimeBlock(now + 1.hours(), now + 3.hours()),
        )
        val chain = TimePeriodChain(periods)

        chain.size shouldBeEqualTo 2
        chain.start shouldBeEqualTo now
    }

    @Test
    fun `invoke with vararg creates chain`() {
        val b1 = TimeBlock(now, now + 1.hours())
        val b2 = TimeBlock(now + 1.hours(), now + 3.hours())
        val chain = TimePeriodChain(b1, b2)

        chain.size shouldBeEqualTo 2
        chain.start shouldBeEqualTo now
        chain.end shouldBeEqualTo now + 3.hours()
    }

    @Test
    fun `headOrNull returns null for empty chain`() {
        val chain = TimePeriodChain()
        chain.headOrNull() shouldBeEqualTo null
    }

    @Test
    fun `headOrNull returns first element`() {
        val chain = TimePeriodChain()
        chain.add(TimeBlock(now, now + 1.hours()))
        chain.headOrNull()?.start shouldBeEqualTo now
    }

    @Test
    fun `lastOrNull returns null for empty chain`() {
        val chain = TimePeriodChain()
        chain.lastOrNull() shouldBeEqualTo null
    }

    @Test
    fun `lastOrNull returns last element`() {
        val chain = TimePeriodChain()
        chain.add(TimeBlock(now, now + 1.hours()))
        chain.add(TimeBlock(now + 1.hours(), now + 3.hours()))
        chain.lastOrNull()?.end shouldBeEqualTo now + 3.hours()
    }

    @Test
    fun `add at index throws UnsupportedOperationException`() {
        val chain = TimePeriodChain()
        assertFailsWith<UnsupportedOperationException> {
            chain.add(0, TimeBlock(now, now + 1.hours()))
        }
    }

    @Test
    fun `remove throws UnsupportedOperationException`() {
        val chain = TimePeriodChain()
        val block = TimeBlock(now, now + 1.hours())
        chain.add(block)
        val before = chain.toList()

        assertFailsWith<UnsupportedOperationException> {
            chain.remove(chain.first())
        }

        chain.toList() shouldBeEqualTo before
    }

    @Test
    fun `set fails before changing the chain`() {
        val chain = chainOfThree()
        val before = chain.toList()

        assertFailsWith<UnsupportedOperationException> {
            chain[0] = TimeBlock(now + 10.hours(), now + 11.hours())
        }

        chain.toList() shouldBeEqualTo before
    }

    @Test
    fun `all destructive list views reject mutation without changing the chain`() {
        val mutationCases = listOf<MutationCase>(
            MutationCase("removeAt") { it.removeAt(0) },
            MutationCase("clear") { it.clear() },
            MutationCase("removeAll") { it.removeAll(listOf(it.first())) },
            MutationCase("retainAll") { it.retainAll(listOf(it.first())) },
            MutationCase("removeIf") { it.removeIf { true } },
            MutationCase("replaceAll") { chain -> chain.replaceAll { it } },
            MutationCase("iterator remove") {
                it.iterator().apply {
                    next()
                    remove()
                }
            },
            MutationCase("listIterator remove") {
                it.listIterator().apply {
                    next()
                    remove()
                }
            },
            MutationCase("listIterator set") {
                it.listIterator().apply {
                    next()
                    set(TimeBlock(now + 10.hours(), now + 11.hours()))
                }
            },
            MutationCase("listIterator add") {
                it.listIterator().add(TimeBlock(now + 10.hours(), now + 11.hours()))
            },
            MutationCase("subList clear") { it.subList(0, 1).clear() },
            MutationCase("subList set") {
                it.subList(0, 1)[0] = TimeBlock(now + 10.hours(), now + 11.hours())
            },
            MutationCase("subList add") {
                it.subList(0, 1).add(TimeBlock(now + 10.hours(), now + 11.hours()))
            },
            MutationCase("indexed addAll") {
                it.addAll(1, listOf(TimeBlock(now + 10.hours(), now + 11.hours())))
            },
            MutationCase("reset") { it.reset() },
            MutationCase("periods add") {
                it.periods.add(TimeBlock(now + 10.hours(), now + 11.hours()))
            },
            MutationCase("periods clear") { it.periods.clear() },
            MutationCase("periods iterator remove") {
                it.periods.iterator().apply {
                    next()
                    remove()
                }
            },
            MutationCase("sortByStart") { it.sortByStart(SortDirection.DESC) },
            MutationCase("sortByEnd") { it.sortByEnd(SortDirection.DESC) },
            MutationCase("sortByDuration") { it.sortByDuration(SortDirection.DESC) },
        )

        mutationCases.forEach { mutationCase ->
            val chain = chainOfThree()
            val before = chain.toList()

            try {
                assertFailsWith<UnsupportedOperationException> {
                    mutationCase.mutate(chain)
                }
            } catch (failure: AssertionError) {
                throw AssertionError("${mutationCase.name} mutation must be rejected.", failure)
            }

            chain.toList() shouldBeEqualTo before
        }
    }

    @Test
    fun `Java default mutation callbacks are rejected before invocation`() {
        val callbacks = listOf<MutationCallbackCase>(
            MutationCallbackCase("removeIf") { chain, invoked ->
                chain.removeIf {
                    invoked()
                    true
                }
            },
            MutationCallbackCase("replaceAll") { chain, invoked ->
                chain.replaceAll {
                    invoked()
                    it
                }
            },
            MutationCallbackCase("sort") { chain, invoked ->
                chain.sortWith { _, _ ->
                    invoked()
                    0
                }
            },
        )

        callbacks.forEach { callbackCase ->
            val chain = chainOfThree()
            val before = chain.map { it.start to it.end }
            var invoked = false

            try {
                assertFailsWith<UnsupportedOperationException> {
                    callbackCase.mutate(chain) { invoked = true }
                }
            } catch (failure: AssertionError) {
                throw AssertionError("${callbackCase.name} must fail before callback invocation.", failure)
            }

            invoked.shouldBeFalse()
            chain.map { it.start to it.end } shouldBeEqualTo before
        }
    }

    @Test
    fun `move shifts entire chain`() {
        val chain = TimePeriodChain()
        chain.add(TimeBlock(now, now + 1.hours()))
        chain.add(TimeBlock(now + 1.hours(), now + 3.hours()))

        val offset = Duration.ofDays(1)
        chain.move(offset)

        chain.start shouldBeEqualTo now.plusDays(1)
        chain.end shouldBeEqualTo now.plusDays(1).plusHours(3)
    }

    @Test
    fun `isMoment is false for non-empty chain`() {
        val chain = TimePeriodChain()
        chain.add(TimeBlock(now, now + 1.hours()))
        chain.isMoment.shouldBeFalse()
    }

    @Test
    fun `addAll from collection appends all periods`() {
        val chain = TimePeriodChain()
        val periods = listOf(
            TimeBlock(now, now + 1.hours()),
            TimeBlock(now + 1.hours(), now + 2.hours()),
            TimeBlock(now + 2.hours(), now + 4.hours()),
        )
        chain.addAll(periods)
        chain shouldHaveSize 3
        chain.end shouldBeEqualTo now + 4.hours()
    }

    @Test
    fun `adding an existing period does not move or duplicate it`() {
        val chain = chainOfThree()
        val before = chain.map { it.start to it.end }

        chain.add(chain.first()).shouldBeFalse()

        chain.map { it.start to it.end } shouldBeEqualTo before
        chain shouldHaveSize 3
    }

    @Test
    fun `adding the chain or its periods to itself is a no-op`() {
        listOf<Pair<String, (TimePeriodChain) -> Collection<ITimePeriod>>>(
            "chain" to { it },
            "periods" to { it.periods },
        ).forEach { (name, source) ->
            val chain = chainOfThree()
            val before = chain.map { it.start to it.end }

            try {
                chain.addAll(source(chain)).shouldBeFalse()
            } catch (failure: AssertionError) {
                throw AssertionError("$name self-add must be a no-op.", failure)
            }

            chain.map { it.start to it.end } shouldBeEqualTo before
            chain shouldHaveSize 3
        }
    }

    @Test
    fun `subclass using inherited periods keeps the mutation guard`() {
        val chain = DerivedTimePeriodChain().apply {
            add(TimeBlock(now, now + 1.hours()))
        }
        val before = chain.map { it.start to it.end }

        assertFailsWith<UnsupportedOperationException> {
            chain.periods.clear()
        }

        chain.map { it.start to it.end } shouldBeEqualTo before
    }

    private fun chainOfThree(): TimePeriodChain = TimePeriodChain(
        listOf(
            TimeBlock(now, now + 1.hours()),
            TimeBlock(now + 1.hours(), now + 3.hours()),
            TimeBlock(now + 3.hours(), now + 6.hours()),
        )
    )

    private data class MutationCase(
        val name: String,
        val mutate: (TimePeriodChain) -> Unit,
    )

    private data class MutationCallbackCase(
        val name: String,
        val mutate: (TimePeriodChain, invoked: () -> Unit) -> Unit,
    )

    private class DerivedTimePeriodChain: TimePeriodChain()
}
