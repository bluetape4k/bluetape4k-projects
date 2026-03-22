package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * 크기 계산 테스트
 */
class SizeTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 크기`() {
        emptyPermutation.isEmpty().shouldBeTrue()
        emptyPermutation.size shouldBeEqualTo 0
    }

    @Test
    fun `고정 순열의 크기`() {
        permutationOf(1).size shouldBeEqualTo 1
        permutationOf(2, 3).size shouldBeEqualTo 2
        permutationOf('a', 'b', 'c').size shouldBeEqualTo 3
        permutationOf(expectedList).size shouldBeEqualTo expectedList.size
    }

    @Test
    fun `지연 순열의 크기`() {
        lazy().size shouldBeEqualTo expectedList.size
    }
}
