package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.utils.MockRandom
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class ULIDStatefulMonotonicTest : AbstractULIDTest() {
    companion object : KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `first call generates new ulid`() {
        val generator = ULID.statefulMonotonic()
        val ulid = generator.nextULID()
        ulid.timestamp shouldBeGreaterThan 0L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `same timestamp increment`() {
        val generator = ULID.statefulMonotonic(factory = ULID.factory(MockRandom(0)))

        val first = generator.nextULID(timestamp = 1000)
        val second = generator.nextULID(timestamp = 1000)

        second.timestamp shouldBeEqualTo first.timestamp
        second shouldBeGreaterThan first
        second.leastSignificantBits shouldBeEqualTo first.leastSignificantBits + 1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `different timestamp increment`() {
        val generator = ULID.statefulMonotonic()
        val first = generator.nextULID(1000)
        val second = generator.nextULID(1001)

        first.timestamp shouldBeEqualTo 1000
        second.timestamp shouldBeEqualTo 1001
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `strict first call`() {
        val generator = ULID.statefulMonotonic()
        val ulid = generator.nextULIDStrict().shouldNotBeNull()
        ulid.timestamp shouldBeGreaterThan 0L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `strict same timestamp increments`() {
        val generator = ULID.statefulMonotonic(factory = ULID.factory(MockRandom(0)))
        val first = generator.nextULIDStrict(timestamp = 1000).shouldNotBeNull()
        val second = generator.nextULIDStrict(timestamp = 1000).shouldNotBeNull()
        second shouldBeGreaterThan first
    }

    @Test
    fun `strict overflow returns null`() {
        val generator = ULID.statefulMonotonic(factory = ULID.factory(MockRandom(-1)))

        // First call: generates ULID with all random bits set (MSB lower 16 bits = 0xFFFF, LSB = -1)
        val first = generator.nextULIDStrict(timestamp = 1000).shouldNotBeNull()

        // Second call: overflow, should return null
        val second = generator.nextULIDStrict(timestamp = 1000).shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `multiple increments`() {
        val generator = ULID.statefulMonotonic(factory = ULID.factory(MockRandom(0)))
        val ulids = (1..10).map { generator.nextULID(timestamp = 500) }
        ulids.sorted() shouldBeEqualTo ulids
    }
}
