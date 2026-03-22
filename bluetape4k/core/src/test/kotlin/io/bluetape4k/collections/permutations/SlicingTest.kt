package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * slice, sliding, grouped 테스트
 */
class SlicingTest: AbstractPermutationTest() {

    // --- slice ---

    @Test
    fun `빈 순열의 slice`() {
        emptyIntPermutation.slice(10, 10).isEmpty().shouldBeTrue()
        emptyIntPermutation.slice(10, 20).isEmpty().shouldBeTrue()
        emptyIntPermutation.slice(0, 0).isEmpty().shouldBeTrue()
        emptyIntPermutation.slice(0, 10).isEmpty().shouldBeTrue()

        assertThrows<IllegalArgumentException> {
            emptyIntPermutation.slice(10, 9)
        }
    }

    @Test
    fun `고정 순열의 slice 예외`() {
        val fixed = permutationOf(1, 2)
        assertThrows<IllegalArgumentException> { fixed.slice(10, 9) }
        assertThrows<IllegalArgumentException> { fixed.slice(-10, 9) }
    }

    @Test
    fun `무한 순열의 slice`() {
        val nums = numbers(0)
        assertThrows<IllegalArgumentException> { nums.slice(-10, 9) }
        nums.slice(3, 7) shouldBeEqualTo permutationOf(3, 4, 5, 6)
    }

    @Test
    fun `비어있지 않은 고정 순열의 slice`() {
        val fixed = permutationOf(1, 2, 3, 4, 5, 6)
        fixed.slice(2, 4) shouldBeEqualTo permutationOf(3, 4)
    }

    @Test
    fun `범위를 넘어서는 slice`() {
        val fixed = permutationOf(1, 2, 3, 4, 5, 6, 7)
        fixed.slice(4, 100) shouldBeEqualTo permutationOf(5, 6, 7)
        fixed.slice(0, 4) shouldBeEqualTo permutationOf(1, 2, 3, 4)
        fixed.slice(0, 100) shouldBeEqualTo permutationOf(1, 2, 3, 4, 5, 6, 7)
        fixed.slice(100, 200).isEmpty().shouldBeTrue()
    }

    // --- sliding ---

    @Test
    fun `빈 순열의 sliding`() {
        emptyPermutation.sliding(1).isEmpty().shouldBeTrue()
        emptyPermutation.sliding(10).isEmpty().shouldBeTrue()
        assertThrows<IllegalArgumentException> { emptyPermutation.sliding(0) }
    }

    @Test
    fun `고정 순열의 sliding`() {
        val fixed = permutationOf(1, 2, 3)
        assertThrows<IllegalArgumentException> { fixed.sliding(0) }

        val sliding = fixed.sliding(4)
        sliding.head shouldBeEqualTo listOf(1, 2, 3)
        sliding.size shouldBeEqualTo 1

        val sliding2 = fixed.sliding(3)
        sliding2.head shouldBeEqualTo listOf(1, 2, 3)
        sliding2.size shouldBeEqualTo 1
    }

    @Test
    fun `고정 순열의 sliding 크기 3`() {
        val fixed = permutationOf(5, 7, 9, 11)
        val sliding = fixed.sliding(3)
        sliding[0] shouldBeEqualTo listOf(5, 7, 9)
        sliding[1] shouldBeEqualTo listOf(7, 9, 11)
        sliding.size shouldBeEqualTo 2

        val sliding2 = fixed.sliding(2)
        sliding2[0] shouldBeEqualTo listOf(5, 7)
        sliding2[1] shouldBeEqualTo listOf(7, 9)
        sliding2[2] shouldBeEqualTo listOf(9, 11)
        sliding2.size shouldBeEqualTo 3
    }

    @Test
    fun `고정 순열의 sliding 크기 1`() {
        val fixed = permutationOf(5, 7, 9, 11)
        val sliding = fixed.sliding(1)
        sliding[0] shouldBeEqualTo listOf(5)
        sliding[1] shouldBeEqualTo listOf(7)
        sliding[2] shouldBeEqualTo listOf(9)
        sliding[3] shouldBeEqualTo listOf(11)
        sliding.size shouldBeEqualTo 4
    }

    @Test
    fun `무한 순열의 sliding`() {
        val primeSeq = primes()
        val sliding = primeSeq.sliding(3)
        sliding[0] shouldBeEqualTo listOf(2, 3, 5)
        sliding[1] shouldBeEqualTo listOf(3, 5, 7)
        sliding[2] shouldBeEqualTo listOf(5, 7, 11)
        sliding[3] shouldBeEqualTo listOf(7, 11, 13)
    }

    // --- grouped ---

    @Test
    fun `빈 순열의 grouped`() {
        emptyPermutation<Any>().grouped(10).isEmpty().shouldBeTrue()
    }

    @Test
    fun `grouped 인자 0이면 예외`() {
        assertThrows<IllegalArgumentException> { emptyPermutation<Any>().grouped(0) }
        assertThrows<IllegalArgumentException> { permutationOf(1, 2, 3).grouped(0) }
    }

    @Test
    fun `고정 순열의 grouped`() {
        val fixed = permutationOf(5, 7, 9)
        val grouped = fixed.grouped(4)
        grouped.head shouldBeEqualTo listOf(5, 7, 9)
        grouped.size shouldBeEqualTo 1
    }

    @Test
    fun `정확히 나누어지는 grouped`() {
        val fixed = permutationOf(5, 7, 9, 11)
        val grouped = fixed.grouped(4)
        grouped.head shouldBeEqualTo listOf(5, 7, 9, 11)
        grouped.size shouldBeEqualTo 1
    }

    @Test
    fun `나머지가 있는 grouped`() {
        val fixed = permutationOf(5, 7, 9, 11)
        val grouped = fixed.grouped(3)
        grouped[0] shouldBeEqualTo listOf(5, 7, 9)
        grouped[1] shouldBeEqualTo listOf(11)
        grouped.size shouldBeEqualTo 2
    }

    @Test
    fun `크기 1 grouped`() {
        val fixed = permutationOf(5, 7, 9)
        val grouped = fixed.grouped(1)
        grouped[0] shouldBeEqualTo listOf(5)
        grouped[1] shouldBeEqualTo listOf(7)
        grouped[2] shouldBeEqualTo listOf(9)
        grouped.size shouldBeEqualTo 3
    }

    @Test
    fun `무한 순열의 grouped`() {
        val primeSeq = primes()
        val grouped = primeSeq.grouped(3)
        grouped[0] shouldBeEqualTo listOf(2, 3, 5)
        grouped[1] shouldBeEqualTo listOf(7, 11, 13)
        grouped[2] shouldBeEqualTo listOf(17, 19, 23)
    }
}
