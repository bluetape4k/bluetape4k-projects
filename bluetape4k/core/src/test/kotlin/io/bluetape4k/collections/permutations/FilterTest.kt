package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * filter 조건 테스트
 */
class FilterTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 filter`() {
        emptyPermutation<Any>().filter { false } shouldBeEqualTo emptyPermutation()
    }

    @Test
    fun `조건에 맞는 요소가 없는 고정 순열`() {
        val fixed = permutationOf(-1, -2, -3)
        fixed.filter { it > 0 }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `조건에 맞는 요소가 없는 지연 고정 순열`() {
        val fixed = permutationOf(-1, -2) { permutationOf(-3, -4) }
        fixed.filter { it > 0 }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `head가 조건에 맞으면 tail을 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val generated = permutationOf("A", "BB") {
            tailEvaluated = true
            emptyPermutation()
        }
        generated.filter { it.isNotEmpty() }
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `head가 조건에 맞지 않으면 tail을 평가해야 한다`() {
        var tailEvaluated = false
        val generated = permutationOf("") {
            tailEvaluated = true
            permutationOf("C")
        }
        generated.filter { it.isNotEmpty() }
        tailEvaluated.shouldBeTrue()
    }

    @Test
    fun `마지막 요소만 조건에 맞는 경우 tail을 여러 번 평가`() {
        val generated = cons("a") {
            cons("bb") {
                cons("ccc") {
                    emptyPermutation()
                }
            }
        }
        val filtered = generated.filter { it.length >= 3 }
        filtered shouldBeEqualTo permutationOf("ccc")
    }

    @Test
    fun `무한 순열에서 여러 요소 필터링`() {
        val naturals = numbers(1)
        val filtered = naturals.filter { it % 3 == 0 }
        filtered.take(4).toList() shouldBeEqualTo listOf(3, 6, 9, 12)
    }
}
