package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * distinct 중복 제거 테스트
 */
class DistinctTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 distinct는 빈 순열`() {
        emptyPermutation<Any>().distinct().isEmpty().shouldBeTrue()
    }

    @Test
    fun `단일 요소 순열의 distinct`() {
        val single = permutationOf(9)
        single.distinct() shouldBeEqualTo permutationOf(9)
    }

    @Test
    fun `두 개의 서로 다른 요소`() {
        val twoDistinct = permutationOf(9, 7)
        twoDistinct.distinct().toList().sorted() shouldBeEqualTo listOf(7, 9)
    }

    @Test
    fun `두 개의 동일한 요소`() {
        val twoSame = permutationOf(9, 9)
        twoSame.distinct() shouldBeEqualTo permutationOf(9)
    }

    @Test
    fun `이미 고유한 순열의 distinct`() {
        val oneToFive = numbers(1).take(5)
        oneToFive.distinct().toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }
}
