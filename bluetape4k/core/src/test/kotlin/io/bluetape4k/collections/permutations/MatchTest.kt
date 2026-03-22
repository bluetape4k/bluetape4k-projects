package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * anyMatch, allMatch, noneMatch, contains, startsWith 통합 테스트
 */
class MatchTest: AbstractPermutationTest() {

    // --- anyMatch ---

    @Test
    fun `빈 순열의 anyMatch`() {
        emptyPermutation<String>().anyMatch { false }.shouldBeFalse()
    }

    @Test
    fun `단일 요소의 anyMatch`() {
        val single = permutationOf(1)
        single.anyMatch { true }.shouldBeTrue()
        single.anyMatch { it > 0 }.shouldBeTrue()
        single.anyMatch { false }.shouldBeFalse()
        single.anyMatch { it < 0 }.shouldBeFalse()
    }

    @Test
    fun `유한 순열의 anyMatch`() {
        val fixed = permutationOf(5, 10, 15)
        fixed.anyMatch { it % 5 == 0 }.shouldBeTrue()
        fixed.anyMatch { it > 100 }.shouldBeFalse()
    }

    @Test
    fun `무한 순열의 anyMatch`() {
        primes().anyMatch { it % 2 != 0 }.shouldBeTrue()
    }

    // --- allMatch ---

    @Test
    fun `빈 순열의 allMatch`() {
        emptyPermutation<String>().allMatch { false }.shouldBeTrue()
    }

    @Test
    fun `유한 순열의 allMatch`() {
        val fixed = permutationOf(5, 10, 15)
        fixed.allMatch { it % 5 == 0 }.shouldBeTrue()
        fixed.allMatch { it <= 10 }.shouldBeFalse()
    }

    @Test
    fun `allMatch에서 head가 불만족하면 tail을 평가하지 않는다`() {
        var tailEvaluated = false
        val lazy = cons("a") {
            tailEvaluated = true
            emptyPermutation()
        }
        lazy.allMatch(String::isEmpty)
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `allMatch에서 head가 만족하면 tail을 평가한다`() {
        var tailEvaluated = false
        val lazy = cons("") {
            tailEvaluated = true
            permutationOf("b")
        }
        lazy.allMatch(String::isEmpty)
        tailEvaluated.shouldBeTrue()
    }

    // --- noneMatch ---

    @Test
    fun `빈 순열의 noneMatch`() {
        emptyPermutation.noneMatch { true }.shouldBeTrue()
    }

    @Test
    fun `유한 순열의 noneMatch`() {
        val fixed = permutationOf(5, 10, 15)
        fixed.noneMatch { it % 5 != 0 }.shouldBeTrue()
        fixed.noneMatch { it > 10 }.shouldBeFalse()
    }

    @Test
    fun `noneMatch에서 head가 만족하면 tail을 평가하지 않는다`() {
        var tailEvaluated = false
        val lazy = cons("a") {
            tailEvaluated = true
            emptyPermutation()
        }
        lazy.noneMatch { it.isNotEmpty() }
        tailEvaluated.shouldBeFalse()
    }

    // --- contains ---

    @Test
    fun `무한 자연수 순열의 contains`() {
        val naturals = numbers(1)
        (naturals as List<Int>).contains(17).shouldBeTrue()
        (naturals.take(1000) as List<Int>).contains(-1).shouldBeFalse()
        (naturals.take(1000) as List<Int>).contains(0).shouldBeFalse()
    }

    @Test
    fun `소수 순열의 contains`() {
        val primeSeq = primes()
        (primeSeq.take(10) as List<Int>).contains(29).shouldBeTrue()
        (primeSeq as List<Int>).contains(997).shouldBeTrue()
        (primeSeq.take(1000) as List<Int>).contains(4).shouldBeFalse()
    }

    // --- startsWith ---

    @Test
    fun `빈 순열의 startsWith`() {
        emptyPermutation.startsWith(emptyPermutation).shouldBeTrue()
        emptyPermutation.startsWith(permutationOf(1)).shouldBeFalse()
    }

    @Test
    fun `고정 순열이 빈 순열로 시작`() {
        permutationOf(1).startsWith(emptyIntPermutation).shouldBeTrue()
    }

    @Test
    fun `고정 순열의 startsWith`() {
        val fixed = permutationOf(1, 2, 3)
        fixed.startsWith(listOf()).shouldBeTrue()
        fixed.startsWith(listOf(1)).shouldBeTrue()
        fixed.startsWith(listOf(1, 2)).shouldBeTrue()
        fixed.startsWith(listOf(1, 2, 3)).shouldBeTrue()
        fixed.startsWith(listOf(1, 2, 3, 4)).shouldBeFalse()
    }

    @Test
    fun `무한 순열의 startsWith`() {
        primes().startsWith(listOf(2, 3, 5, 7)).shouldBeTrue()
        primes().startsWith(listOf(2, 3, 4)).shouldBeFalse()
    }
}
