package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * take(n) 테스트
 */
class TakeTest: AbstractPermutationTest() {

    @Test
    fun `음수 인자로 take 호출 시 예외 발생`() {
        val seq = permutationOf(1, 2, 3)
        assertThrows<IllegalArgumentException> {
            seq.limit(-1)
        }
    }

    @Test
    fun `빈 순열의 take`() {
        emptyPermutation.limit(10).isEmpty().shouldBeTrue()
    }

    @Test
    fun `고정 순열의 take`() {
        val fixed = permutationOf(1, 2, 3, 4, 5)
        fixed.limit(3) shouldBeEqualTo permutationOf(1, 2, 3)
    }

    @Test
    fun `무한 순열의 take`() {
        numbers(1).limit(3) shouldBeEqualTo permutationOf(1, 2, 3)
    }

    @Test
    fun `take 시 tail을 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val infinite = permutationOf(1) {
            tailEvaluated = true
            emptyPermutation()
        }
        infinite.take(10)
        tailEvaluated.shouldBeFalse()
    }
}
