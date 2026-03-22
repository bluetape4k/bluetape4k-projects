package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * scan 누적 테스트
 */
class ScanTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 scan은 초기값만 포함`() {
        val scanned: Permutation<Int> = emptyIntPermutation.scan(0) { acc, c -> acc + c }
        scanned shouldBeEqualTo permutationOf(0)
    }

    @Test
    fun `고정 순열의 scan`() {
        val fixed = permutationOf(1, 2, 3, 4)
        val scanned = fixed.scan(0) { acc, c -> acc + c }
        scanned shouldBeEqualTo permutationOf(0, 1, 3, 6, 10)
    }

    @Test
    fun `문자열 고정 순열의 scan`() {
        val fixed = continually("*").take(5)
        val scanned = fixed.scan("") { acc, c -> acc + c }
        scanned shouldBeEqualTo permutationOf("", "*", "**", "***", "****", "*****")
    }

    @Test
    fun `무한 순열의 scan`() {
        val primeSeq = primes()
        val scanned = primeSeq.scan(1) { acc, c -> acc * c }
        scanned.take(4) shouldBeEqualTo permutationOf(1, 1 * 2, 1 * 2 * 3, 1 * 2 * 3 * 5)
    }
}
