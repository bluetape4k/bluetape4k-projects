package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * toString 포맷 테스트
 */
class ToStringTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 toString`() {
        emptyPermutation.toString() shouldBeEqualTo "[]"
    }

    @Test
    fun `고정 순열의 toString`() {
        permutationOf('x').toString() shouldBeEqualTo "[x]"
        permutationOf('x', 'y').toString() shouldBeEqualTo "[x, y]"
        permutationOf('x', 'y', 'z').toString() shouldBeEqualTo "[x, y, z]"
    }

    @Test
    fun `무한 순열의 첫 요소만 포함한 toString`() {
        numbers(1).toString() shouldBeEqualTo "[1, ?]"

        val nums = numbers(0)
        nums[4]
        nums.toString() shouldBeEqualTo "[0, 1, 2, 3, 4, ?]"
    }

    @Test
    fun `혼합 순열의 toString`() {
        val notFull = permutationOf(1, 2) { permutationOf(3) }
        notFull.toString() shouldBeEqualTo "[1, 2, ?]"
    }

    @Test
    fun `종료된 순열의 toString`() {
        val notFull: Permutation<Int> = permutationOf(1, 2) { emptyPermutation() }

        notFull.toString() shouldBeEqualTo "[1, 2, ?]"

        notFull.forEach { }
        notFull.toString() shouldBeEqualTo "[1, 2]"
    }
}
