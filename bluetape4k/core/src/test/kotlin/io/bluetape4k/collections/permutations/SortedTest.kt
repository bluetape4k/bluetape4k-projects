package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * sorted 정렬 테스트
 */
class SortedTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 정렬`() {
        emptyPermutation.sorted().isEmpty().shouldBeTrue()
    }

    @Test
    fun `단일 순열의 정렬`() {
        permutationOf(17).sorted() shouldBeEqualTo permutationOf(17)
    }

    @Test
    fun `고정 순열의 정렬`() {
        val fixed = permutationOf(17, 3, 15, 9, 4)
        fixed.sorted() shouldBeEqualTo permutationOf(3, 4, 9, 15, 17)
    }

    @Test
    fun `지연 유한 순열의 정렬`() {
        val lazySeq = lazy()
        lazySeq.sorted() shouldBeEqualTo permutationOf(expectedList.sorted())
    }

    @Test
    fun `문자열 순열의 커스텀 정렬`() {
        val fixed = permutationOf("ab", "c", "", "ghjkl", "def")
        val sorted = fixed.sorted { s1, s2 -> s1.length - s2.length }
        sorted shouldBeEqualTo permutationOf("", "c", "ab", "def", "ghjkl")
    }
}
