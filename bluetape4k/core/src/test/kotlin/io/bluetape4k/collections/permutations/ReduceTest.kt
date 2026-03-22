package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

/**
 * reduce 연산 테스트
 */
class ReduceTest: AbstractPermutationTest() {

    private val sum: (Int, Int) -> Int get() = { a, b -> a + b }
    private val product: (Int, Int) -> Int get() = { a, b -> a * b }

    @Test
    fun `빈 순열의 합계`() {
        emptyIntPermutation.reduce(0, sum) shouldBeEqualTo 0
    }

    @Test
    fun `고정 순열의 합계`() {
        val fixed = permutationOf(1, 2, 4, 7)
        fixed.reduce(0, sum) shouldBeEqualTo (1 + 2 + 4 + 7)
    }

    @Test
    fun `지연 고정 순열의 합계`() {
        val lazy = cons(1) { cons(2) { cons(4) { permutationOf(7) } } }
        lazy.reduce(0, sum) shouldBeEqualTo (1 + 2 + 4 + 7)
    }

    @Test
    fun `빈 순열의 곱`() {
        emptyIntPermutation.reduce(product).shouldBeNull()
        emptyIntPermutation.reduce(1, product) shouldBeEqualTo 1
    }

    @Test
    fun `고정 순열의 곱`() {
        val fixed = permutationOf(1, 2, 4, 7)
        fixed.reduce(1, product) shouldBeEqualTo (1 * 2 * 4 * 7)
    }

    @Test
    fun `지연 고정 순열의 곱`() {
        val lazy = cons(1) { cons(2) { cons(4) { permutationOf(7) } } }
        lazy.reduce(1, product) shouldBeEqualTo (1 * 2 * 4 * 7)
    }

    @Test
    fun `빈 순열의 문자열 길이 합계는 0`() {
        val totalSum = emptyPermutation<String>().reduce(0) { acc, i -> acc + i.length }
        totalSum shouldBeEqualTo 0
    }

    @Test
    fun `고정 순열의 문자열 길이 합계`() {
        val fixed = permutationOf("1", "22", "4444", "7777777")
        fixed.reduce(0) { acc, i -> acc + i.length } shouldBeEqualTo (1 + 2 + 4 + 7)
    }

    @Test
    fun `지연 순열의 문자열 길이 합계`() {
        val lazy = cons("1") { cons("22") { cons("4444") { permutationOf("7777777") } } }
        lazy.reduce(0) { acc, i -> acc + i.length } shouldBeEqualTo (1 + 2 + 4 + 7)
    }
}
