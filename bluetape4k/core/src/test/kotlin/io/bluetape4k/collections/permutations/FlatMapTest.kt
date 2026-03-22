package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * flatMap 테스트
 */
class FlatMapTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 flatMap은 빈 순열`() {
        val empty = emptyPermutation<Int>()
        empty.flatMap { listOf(it, -it) }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `head 평탄화`() {
        val raw = permutationOf(1, 2)
        val flat = raw.flatMap { listOf(it, -it) }
        flat shouldBeEqualTo permutationOf(1, -1, 2, -2)
    }

    @Test
    fun `무한 스트림의 지연 평탄화`() {
        val raw = numbers(1)
        val flat = raw.flatMap { listOf(it, 0, -it) }
        flat.take(10) shouldBeEqualTo permutationOf(1, 0, -1, 2, 0, -2, 3, 0, -3, 4)
    }

    @Test
    fun `head만 평탄화하고 tail은 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val raw = permutationOf(1) {
            tailEvaluated = true
            emptyPermutation()
        }
        val flat = raw.flatMap { listOf(it, 0, -it) }

        flat[0] shouldBeEqualTo 1
        flat[1] shouldBeEqualTo 0
        flat[2] shouldBeEqualTo -1
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `첫 flatMap 결과가 비어있으면 계속 평가`() {
        val from = -10
        val raw = numbers(from)
        val flat = raw.flatMap { flatMapFunc(it) }
        flat.take(10) shouldBeEqualTo permutationOf(2, 3, 4, 5, 6, 7, 0, 0, 0, 0)
    }

    private fun flatMapFunc(i: Int): Iterable<Int> {
        if (i <= 0) return listOf()
        return when (i) {
            1 -> listOf(2)
            2 -> listOf(3, 4)
            3 -> listOf(5, 6, 7)
            else -> listOf(0, 0)
        }
    }
}
