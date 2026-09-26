package io.bluetape4k.math.commons

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PrecisionTest {

    companion object: KLogging() {
        private const val DEFAULT_EPSILON = 1e-11
    }

    @Test
    fun `epsilon of double type`() {
        log.trace { "1.0.epsilon=${1.0.epsilon()}" }
        12.0.epsilon() shouldBeEqualTo 0.0

        val one11 = 1.00000001
        val one11Epsilon = one11.epsilon()
        log.trace { "one11.epsiolon=$one11Epsilon" }
        one11.epsilon().approximateEqual(one11 - one11.toLong(), DEFAULT_EPSILON).shouldBeTrue()
    }

    @Test
    fun `epsilone of random value over 1`() {
        repeat(100) {
            val double = Random.nextDouble(1.0, 10.0)
            val epsilon = double.epsilon()
            log.trace { "double=$double, epsilon=$epsilon" }

            epsilon.approximateEqual(double - double.toLong(), DEFAULT_EPSILON).shouldBeTrue()
        }
    }

    @Test
    fun `epsilone of random value 0~1`() {
        repeat(100) {
            val double = Random.nextDouble(0.0, 1.0)
            val epsilon = double.epsilon()
            log.trace { "double=$double, epsilon=$epsilon" }

            epsilon.approximateEqual(1.0 - double, DEFAULT_EPSILON).shouldBeTrue()
        }
    }

    @Test
    fun `epsilon of negative value`() {
        repeat(100) {
            val double = Random.nextDouble(-10.0, -1.0)
            val epsilon = double.epsilon()
            log.trace { "double=$double, epsilon=$epsilon" }

            epsilon.approximateEqual((double - 1.0).toLong() - double, DEFAULT_EPSILON).shouldBeTrue()
        }
    }

    @Test
    fun `epsilone of random value minus 0~1`() {
        repeat(100) {
            val double = Random.nextDouble(-1.0, 0.0)
            val epsilon = double.epsilon()
            log.trace { "double=$double, epsilon=$epsilon" }

            epsilon.approximateEqual(double.abs() + 1.0, DEFAULT_EPSILON).shouldBeTrue()
        }
    }
}
