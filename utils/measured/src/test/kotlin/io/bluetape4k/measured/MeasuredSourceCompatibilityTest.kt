package io.bluetape4k.measured

import io.bluetape4k.assertions.shouldBeNear
import org.junit.jupiter.api.Test

class MeasuredSourceCompatibilityTest {

    @Test
    fun `binary size per time keeps the generic ratio return type`() {
        val ratio = baseBinarySizeRate()

        ratio.amount.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `mass and acceleration keep the semantic force return type`() {
        val force = baseMassAccelerationForce()

        force.amount.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `legacy JVM method remains callable through its stable binary name`() {
        val rate = MeasuredBinaryCompatibilityFixture.invokeLegacyBinaryMethod(
            1.megabytes10(),
            1.seconds(),
        )

        (rate `in` DataRate.megaBytesPerSecond).shouldBeNear(1.0, 1e-10)
    }
}
