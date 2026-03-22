package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * map 변환 테스트
 */
class MapTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 map은 빈 순열`() {
        emptyPermutation<Int>().isEmpty().shouldBeTrue()
    }

    @Test
    fun `단일 요소 고정 순열의 map`() {
        val chars = permutationOf('a')
        val toUpper = chars.map(Char::uppercaseChar)
        toUpper shouldBeEqualTo permutationOf('A')
    }

    @Test
    fun `여러 요소 고정 순열의 map`() {
        val chars = permutationOf('a', 'b', 'c')
        val toUpper = chars.map(Char::uppercaseChar)
        toUpper shouldBeEqualTo permutationOf('A', 'B', 'C')
    }

    @Test
    fun `무한 순열의 map`() {
        val naturals = numbers(1)
        val multiplied = naturals.map { it * 10 }
        multiplied.take(4) shouldBeEqualTo permutationOf(10, 20, 30, 40)
    }

    @Test
    fun `map 시 tail을 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val seq = cons(17) {
            tailEvaluated = true
            emptyPermutation()
        }
        seq.map(Int::toString)
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `map으로 head 변환`() {
        val seq = cons(17) { emptyPermutation() }
        val strings = seq.map(Int::toString)
        strings.head shouldBeEqualTo "17"
    }
}
