package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class OpenOpenRangeTest {

    companion object: KLogging()

    @Test
    fun `ClosedRange to OpenOpenRange`() {
        (0..10).toOpenOpenRange() shouldBeEqualTo DefaultOpenOpenRange(0, 10)
        (1.0..2.0).toOpenOpenRange() shouldBeEqualTo DefaultOpenOpenRange(1.0, 2.0)
    }

    @Test
    fun `if last is smaller than or equals to first`() {
        DefaultOpenOpenRange(1, 2).isEmpty().shouldBeFalse()

        DefaultOpenOpenRange(1, 1).isEmpty().shouldBeTrue()
        DefaultOpenOpenRange(2, 1).isEmpty().shouldBeTrue()
    }

    @Test
    fun `range contains element`() {
        val ten = DefaultOpenOpenRange(0, 10)
        ten.contains(3).shouldBeTrue()
        ten.contains(9).shouldBeTrue()

        ten.contains(0).shouldBeFalse()
        ten.contains(10).shouldBeFalse()
    }

    @Test
    fun `range contains range`() {
        val larger = DefaultOpenOpenRange(0, 10)
        val inner = DefaultOpenOpenRange(4, 8)
        val outer = DefaultOpenOpenRange(20, 30)
        val intersect = DefaultOpenOpenRange(5, 15)

        larger.contains(larger).shouldBeTrue()
        larger.contains(inner).shouldBeTrue()
        larger.contains(outer).shouldBeFalse()
        larger.contains(intersect).shouldBeFalse()
    }
}
