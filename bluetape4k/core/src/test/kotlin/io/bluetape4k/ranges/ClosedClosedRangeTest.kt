package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ClosedClosedRangeTest {

    companion object: KLogging()

    @Test
    fun `range to ClosedClosedRange`() {
        (0..10).toClosedClosedRange() shouldBeEqualTo DefaultClosedClosedRange(0, 10)
        (1.0..2.0).toClosedClosedRange() shouldBeEqualTo DefaultClosedClosedRange(1.0, 2.0)
    }

    @Test
    fun `if last is smaller than or equals to first`() {
        DefaultClosedClosedRange(1, 2).isEmpty().shouldBeFalse()

        DefaultClosedClosedRange(1, 1).isEmpty().shouldBeTrue()
        DefaultClosedClosedRange(2, 1).isEmpty().shouldBeTrue()
    }

    @Test
    fun `range contains element`() {
        val ten = DefaultClosedClosedRange(0, 10)
        ten.contains(3).shouldBeTrue()
        ten.contains(9).shouldBeTrue()

        ten.contains(0).shouldBeTrue()
        ten.contains(10).shouldBeTrue()
    }

    @Test
    fun `range contains range`() {
        val larger = DefaultClosedClosedRange(0, 10)
        val inner = DefaultClosedClosedRange(4, 8)
        val outer = DefaultClosedClosedRange(20, 30)
        val intersect = DefaultClosedClosedRange(5, 15)

        larger.contains(larger).shouldBeTrue()
        larger.contains(inner).shouldBeTrue()
        larger.contains(outer).shouldBeFalse()
        larger.contains(intersect).shouldBeFalse()
    }
}
