package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test


class ClosedOpenRangeTest {

    companion object: KLogging()

    @Test
    fun `range to ClosedOpenRange`() {
        (0..10).toClosedOpenRange() shouldBeEqualTo DefaultClosedOpenRange(0, 10)
        (1.0..2.0).toClosedOpenRange() shouldBeEqualTo DefaultClosedOpenRange(1.0, 2.0)
    }

    @Test
    fun `if last is smaller than or equals to first`() {
        DefaultClosedOpenRange(1, 2).isEmpty().shouldBeFalse()

        DefaultClosedOpenRange(1, 1).isEmpty().shouldBeTrue()
        DefaultClosedOpenRange(2, 1).isEmpty().shouldBeTrue()
    }

    @Test
    fun `build ClosedOpenRange by until operator`() {
        (1 until 3) shouldBeInstanceOf ClosedOpenRange::class
        (3 until 1) shouldBeInstanceOf ClosedOpenRange::class
    }

    @Test
    fun `range contains element`() {
        val ten = (0 until 10)
        ten.contains(3).shouldBeTrue()
        ten.contains(9).shouldBeTrue()
        ten.contains(0).shouldBeTrue()
        ten.contains(10).shouldBeFalse()
    }

    @Test
    fun `range contains range`() {
        val larger = (0 until 10)
        val inner = (4 until 8)
        val outer = (20 until 30)
        val intersect = (5 until 15)

        larger.contains(larger).shouldBeTrue()
        larger.contains(inner).shouldBeTrue()
        larger.contains(outer).shouldBeFalse()
        larger.contains(intersect).shouldBeFalse()
    }
}
