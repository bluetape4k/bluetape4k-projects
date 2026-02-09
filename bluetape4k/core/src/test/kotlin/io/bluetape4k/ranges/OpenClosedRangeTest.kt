package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class OpenClosedRangeTest {

    companion object: KLogging()

    @Test
    fun `range to OpenClosedRange`() {
        (0..10).toOpenClosedRange() shouldBeEqualTo DefaultOpenClosedRange(0, 10)
        (1.0..2.0).toOpenClosedRange() shouldBeEqualTo DefaultOpenClosedRange(1.0, 2.0)
    }

    @Test
    fun `create by factory function`() {
        openClosedRangeOf(0, 10) shouldBeEqualTo DefaultOpenClosedRange(0, 10)
        openClosedRangeOf(1.0, 2.0) shouldBeEqualTo DefaultOpenClosedRange(1.0, 2.0)
    }

    @Test
    fun `boundary properties`() {
        val range = openClosedRangeOf(0, 10)
        range.isStartInclusive.shouldBeFalse()
        range.isEndInclusive.shouldBeTrue()
    }

    @Test
    fun `if last is smaller than or equals to first`() {
        DefaultOpenClosedRange(1, 2).isEmpty().shouldBeFalse()

        DefaultOpenClosedRange(1, 1).isEmpty().shouldBeTrue()
        DefaultOpenClosedRange(2, 1).isEmpty().shouldBeTrue()
    }

    @Test
    fun `range contains element`() {
        val ten = DefaultOpenClosedRange(0, 10)
        ten.contains(3).shouldBeTrue()
        ten.contains(9).shouldBeTrue()

        ten.contains(0).shouldBeFalse()
        ten.contains(10).shouldBeTrue()
    }

    @Test
    fun `range contains range`() {
        val larger = DefaultOpenClosedRange(0, 10)
        val inner = DefaultOpenClosedRange(4, 8)
        val outer = DefaultOpenClosedRange(20, 30)
        val intersect = DefaultOpenClosedRange(5, 15)

        larger.contains(larger).shouldBeTrue()
        larger.contains(inner).shouldBeTrue()
        larger.contains(outer).shouldBeFalse()
        larger.contains(intersect).shouldBeFalse()
    }
}