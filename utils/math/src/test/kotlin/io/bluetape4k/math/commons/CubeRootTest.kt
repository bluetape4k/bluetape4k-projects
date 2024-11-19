package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.random.Random

class CubeRootTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    @Test
    fun `cube root for positive value`() {
        repeat(REPEAT_SIZE) {
            cubeRoot(it.toDouble().pow(3)) shouldBeEqualTo it.toDouble()
        }
    }

    @Test
    fun `cube root for negative value`() {
        repeat(REPEAT_SIZE) {
            cubeRoot(it.unaryMinus().toDouble().pow(3.0)) shouldBeEqualTo it.unaryMinus().toDouble()
        }
    }

    @Test
    fun `cube root for random double values`() {
        repeat(REPEAT_SIZE * 100) {
            val value = Random.nextDouble(-100.0, 100.0)
            cubeRoot(value.pow(3.0)).approximateEqual(value, 1e-5).shouldBeTrue()
        }
    }
}
