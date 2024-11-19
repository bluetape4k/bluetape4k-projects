package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.special.MAX_FACTORIAL_NUMBER
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CombinationTest {

    companion object: KLogging()

    @Test
    fun `calc combination`() {
        combination(1, -1) shouldBeEqualTo 1
        combination(1, 0) shouldBeEqualTo 1
        combination(1, 1) shouldBeEqualTo 1

        combination(10, 1) shouldBeEqualTo 10
        combination(10, 2) shouldBeEqualTo 45
        combination(10, 5) shouldBeEqualTo 252
    }

    @Test
    fun `calc combination by long type`() {
        longCombination(1, -1) shouldBeEqualTo 1L
        longCombination(1, 0) shouldBeEqualTo 1L
        longCombination(1, 1) shouldBeEqualTo 1L

        longCombination(10, 1) shouldBeEqualTo 10L
        longCombination(10, 2) shouldBeEqualTo 45L
        longCombination(10, 5) shouldBeEqualTo 252L

        assertFailsWith<AssertionError> {
            longCombination(1, 18)
        }
    }

    @Test
    fun `variation operation`() {
        variations(-1, 0) shouldBeEqualTo 0.0
        variations(-1, -1) shouldBeEqualTo 0.0
        variations(0, -1) shouldBeEqualTo 0.0
        variations(1, 2) shouldBeEqualTo 0.0

        variations(5, 1) shouldBeEqualTo 5.0
        variations(5, 2) shouldBeEqualTo 20.0
        variations(5, 3) shouldBeEqualTo 60.0
        variations(5, 4) shouldBeEqualTo 120.0
        variations(5, 5) shouldBeEqualTo 120.0

    }

    @Test
    fun `variations without repeatition`() {
        variationsWithRepetition(-1, 0) shouldBeEqualTo 0.0
        variationsWithRepetition(-1, -1) shouldBeEqualTo 0.0
        variationsWithRepetition(0, -1) shouldBeEqualTo 0.0

        variationsWithRepetition(5, 1) shouldBeEqualTo 5.0
        variationsWithRepetition(5, 2) shouldBeEqualTo 25.0
        variationsWithRepetition(5, 3) shouldBeEqualTo 125.0
        variationsWithRepetition(5, 4) shouldBeEqualTo 625.0
        variationsWithRepetition(5, 5) shouldBeEqualTo 3125.0
    }

    @Test
    fun `combinations operation`() {
        combinations(-1, 0) shouldBeEqualTo 0.0
        combinations(-1, -1) shouldBeEqualTo 0.0
        combinations(0, -1) shouldBeEqualTo 0.0
        combinations(1, 2) shouldBeEqualTo 0.0

        combinations(5, 1) shouldBeEqualTo combination(5, 1).toDouble().apply { println(this) }     // 5.0
        combinations(5, 2) shouldBeEqualTo combination(5, 2).toDouble().apply { println(this) }     // 10.0
        combinations(5, 3) shouldBeEqualTo combination(5, 3).toDouble().apply { println(this) }     // 10.0
        combinations(5, 4) shouldBeEqualTo combination(5, 4).toDouble().apply { println(this) }     // 5.0
        combinations(5, 5) shouldBeEqualTo combination(5, 5).toDouble().apply { println(this) }     // 1.0
    }

    @Test
    fun `combinations with repeatition`() {
        combinationsWithRepetition(-1, 0) shouldBeEqualTo 0.0
        combinationsWithRepetition(-1, -1) shouldBeEqualTo 0.0
        combinationsWithRepetition(0, -1) shouldBeEqualTo 0.0
        combinationsWithRepetition(0, 2) shouldBeEqualTo 0.0

        combinationsWithRepetition(5, 1) shouldBeEqualTo 5.0
        combinationsWithRepetition(5, 2) shouldBeEqualTo 15.0
        combinationsWithRepetition(5, 3) shouldBeEqualTo 35.0
        combinationsWithRepetition(5, 4) shouldBeEqualTo 70.0
        combinationsWithRepetition(5, 5) shouldBeEqualTo 126.0
    }

    fun `permutations operation`() {
        assertFailsWith<IllegalArgumentException> {
            permutations(-1)
        }
        permutations(MAX_FACTORIAL_NUMBER + 1) shouldBeEqualTo Double.POSITIVE_INFINITY

        permutations(1) shouldBeEqualTo 1 * permutations(0)
        permutations(5) shouldBeEqualTo 5 * permutations(4)
        permutations(10) shouldBeEqualTo 10 * permutations(9)
    }
}
