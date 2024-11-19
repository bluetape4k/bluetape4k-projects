package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class GeometricsTest {

    companion object: KLogging()

    @Test
    fun `hypot with zero`() {
        hypot(0.0, -2.0) shouldBeEqualTo 2.0
        hypot(-2.0, 0.0) shouldBeEqualTo 2.0
        hypot(0.0, 2.0) shouldBeEqualTo 2.0
        hypot(2.0, 0.0) shouldBeEqualTo 2.0
    }

    @Test
    fun `hypot with values`() {
        hypot(3.0, 4.0) shouldBeEqualTo 5.0
        hypot(3.0, -4.0) shouldBeEqualTo 5.0
        hypot(-3.0, 4.0) shouldBeEqualTo 5.0
        hypot(-3.0, -4.0) shouldBeEqualTo 5.0
    }

    @Test
    fun `hypot2 with zero`() {
        hypot2(0.0, -2.0) shouldBeEqualTo 2.0
        hypot2(-2.0, 0.0) shouldBeEqualTo 2.0
        hypot2(0.0, 2.0) shouldBeEqualTo 2.0
        hypot2(2.0, 0.0) shouldBeEqualTo 2.0
    }

    @Test
    fun `hypot2 with values`() {
        hypot2(3.0, 4.0).clamp(5.0, 1e-5) shouldBeEqualTo 5.0
        hypot2(3.0, -4.0).clamp(5.0, 1e-5) shouldBeEqualTo 5.0
        hypot2(-3.0, 4.0).clamp(5.0, 1e-5) shouldBeEqualTo 5.0
        hypot2(-3.0, -4.0).clamp(5.0, 1e-5) shouldBeEqualTo 5.0
    }

    @Test
    fun `distance with two points`() {
        distance(0.0, 0.0, 3.0, 4.0) shouldBeEqualTo 5.0
        distance(0.0, 0.0, 3.0, -4.0) shouldBeEqualTo 5.0
        distance(0.0, 0.0, -3.0, 4.0) shouldBeEqualTo 5.0
        distance(0.0, 0.0, -3.0, -4.0) shouldBeEqualTo 5.0
    }
}
