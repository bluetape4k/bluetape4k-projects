package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ApproximateTest {

    companion object: KLogging()

    @Test
    fun `approximate equal by double value`() {
        val lhs = 0.11111
        val rhs = 0.1111112

        lhs.approximateEqual(rhs, 1e-5).shouldBeTrue()
        lhs.approximateEqual(rhs, 1e-6).shouldBeFalse()
        lhs.approximateEqual(rhs).shouldBeFalse()
    }

    @Test
    fun `approximate equal by float value`() {
        val lhs = 0.11111F
        val rhs = 0.1111112F

        lhs.approximateEqual(rhs, 1e-5F).shouldBeTrue()
        lhs.approximateEqual(rhs, 1e-6F).shouldBeFalse()
        lhs.approximateEqual(rhs).shouldBeFalse()
    }

    @Test
    fun `approximate equal by BigDecimal value`() {
        val lhs = 0.11111.toBigDecimal()
        val rhs = 0.1111112.toBigDecimal()

        lhs.approximateEqual(rhs, 1e-5.toBigDecimal()).shouldBeTrue()
        lhs.approximateEqual(rhs, 1e-6.toBigDecimal()).shouldBeFalse()
        lhs.approximateEqual(rhs).shouldBeFalse()
    }
}
