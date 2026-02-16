package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.random.Random

class AbsTest {

    companion object: KLogging() {
        private const val ITEM_SIZE = 100
    }

    @Test
    fun `absolute value for number`() {
        (-1.0).abs() shouldBeEqualTo 1.0
        (-1.0F).abs() shouldBeEqualTo 1.0F
        (-1L).abs() shouldBeEqualTo 1L
        (-1).abs() shouldBeEqualTo 1
    }

    @Test
    fun `absolute value for doubles`() {
        val numbers = List(ITEM_SIZE) {
            Random.nextDouble(-10.0, 10.0)
        }

        numbers.any { it < 0.0 }.shouldBeTrue()
        numbers.abs().all { it >= 0.0 }.shouldBeTrue()
    }

    @Test
    fun `absolute value for floats`() {
        val numbers = List(ITEM_SIZE) {
            Random.nextDouble(-10.0, 10.0).toFloat()
        }

        numbers.any { it < 0.0 }.shouldBeTrue()
        numbers.abs().all { it >= 0.0 }.shouldBeTrue()
    }

    @Test
    fun `absolute value for BigDecimal`() {
        val numbers = List(ITEM_SIZE) {
            Random.nextDouble(-10.0, 10.0).toBigDecimal()
        }

        numbers.any { it < BigDecimal.ZERO }.shouldBeTrue()
        numbers.abs().all { it >= BigDecimal.ZERO }.shouldBeTrue()
    }
}
