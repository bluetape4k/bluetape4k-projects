package io.bluetape4k.math

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RandomSupportTest {

    enum class Dice {
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX
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

        assertFailsWith<IllegalArgumentException> {
            WeightedCoin(-1.0)
        }
        assertFailsWith<IllegalArgumentException> {
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
        List(10) { onlySix.roll() }.distinct() shouldContainSame listOf(Dice.SIX)
    }

    @Test
    fun `empty weighted dice`() {
        assertFailsWith<IllegalArgumentException> {
            WeightedDice<Dice>()
        }
    }

    @Test
    fun `빈 리스트에서 randomFirst 호출 시 예외 발생`() {
        assertFailsWith<NoSuchElementException> {
            emptyList<Int>().randomFirst()
        }
    }

    @Test
    fun `빈 리스트에서 randomFirstOrNull은 null 반환`() {
        val result = emptyList<Int>().randomFirstOrNull()
        (result == null).shouldBeTrue()
    }

    @Test
    fun `randomDistinct는 중복 없는 결과를 반환`() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val result = list.randomDistinct(5)
        (result.size == result.distinct().size).shouldBeTrue()
        result.all { it in list }.shouldBeTrue()
    }

    @Test
    fun `randomDistinct 빈 리스트는 빈 결과 반환`() {
        val result = emptyList<Int>().randomDistinct(5)
        result.isEmpty().shouldBeTrue()
    }

    @Test
    fun `weightedCoinFlip 범위 밖 확률은 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            weightedCoinFlip(-0.1)
        }
        assertFailsWith<IllegalArgumentException> {
            weightedCoinFlip(1.1)
        }
    }

    @Test
    fun `weightedCoinFlip 경계값 동작 확인`() {
        weightedCoinFlip(0.0).shouldBeFalse()
        weightedCoinFlip(1.0).shouldBeTrue()
    }
}
