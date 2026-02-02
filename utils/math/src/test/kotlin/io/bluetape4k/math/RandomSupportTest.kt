package io.bluetape4k.math

import io.bluetape4k.collections.eclipse.fastList
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RandomSupportTest {

    enum class Dice {
        ONE, TWO, THREE, FOUR, FIVE, SIX
    }

    @Test
    fun `select random element`() {
        val list = listOf(1, 2, 3, 4, 5)
        list.randomFirst() shouldBeIn list
    }

    @Test
    fun `fill random elements`() {
        val list = listOf(1, 2, 3, 4, 5)

        val randomized = list.random(10)
        randomized.all { it in list }.shouldBeTrue()
    }

    @Test
    fun `weighted coin flip with probability`() {
        val allFalse = WeightedCoin(0.0)
        allFalse.flip().shouldBeFalse()

        val allTrue = WeightedCoin(1.0)
        allTrue.flip().shouldBeTrue()

        assertFailsWith<AssertionError> {
            WeightedCoin(-1.0)
        }
        assertFailsWith<AssertionError> {
            WeightedCoin(1.1)
        }
    }

    @Test
    fun `weighted dice`() {
        val dice = WeightedDice(
            Dice.ONE to 0.1,
            Dice.TWO to 0.1,
            Dice.THREE to 0.1,
            Dice.FOUR to 0.2,
            Dice.FIVE to 0.2,
            Dice.SIX to 0.2,
        )
        dice.roll() shouldBeIn Dice.entries.toTypedArray()
        dice.roll() shouldBeIn Dice.entries.toTypedArray()

        val onlySix = WeightedDice(
            Dice.ONE to 0.0,
            Dice.TWO to 0.0,
            Dice.THREE to 0.0,
            Dice.FOUR to 0.0,
            Dice.FIVE to 0.0,
            Dice.SIX to 1.0,
        )
        fastList(10) { onlySix.roll() }.distinct() shouldContainSame listOf(Dice.SIX)
    }

    @Test
    fun `empty weighted dice`() {
        assertFailsWith<AssertionError> {
            WeightedDice<Dice>().roll()
        }
    }
}
