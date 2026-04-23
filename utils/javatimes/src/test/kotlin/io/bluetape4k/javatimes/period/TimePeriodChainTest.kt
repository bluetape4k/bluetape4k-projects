package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.hours
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

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
        assertFailsWith<UnsupportedOperationException> {
            chain.remove(chain.first())
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
}
