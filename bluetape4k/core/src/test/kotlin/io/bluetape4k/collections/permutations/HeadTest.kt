package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * head 접근 테스트
 */
class HeadTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 head 접근 시 예외 발생`() {
        val empty = emptyPermutation<Any>()
        assertThrows<NoSuchElementException> {
            empty.head
        }
    }

    @Test
    fun `고정 순열에서 head 가져오기`() {
        permutationOf(1).head shouldBeEqualTo 1
        permutationOf(2, 3).head shouldBeEqualTo 2
        permutationOf(4, 5, 6).head shouldBeEqualTo 4
        permutationOf(listOf(7, 8, 9, 10)).head shouldBeEqualTo 7
    }

    @Test
    fun `동적 순열에서 head 가져오기`() {
        permutationOf(1, tail()).head shouldBeEqualTo 1
        permutationOf(2, 3, tail()).head shouldBeEqualTo 2
        permutationOf(4, 5, 6, tail()).head shouldBeEqualTo 4
        concat(listOf(7, 8, 9, 10), tail()).head shouldBeEqualTo 7
    }

    @Test
    fun `동적 순열에서 tail의 head 가져오기`() {
        permutationOf(1, tail()).tail.head shouldBeEqualTo 42
        permutationOf(2, 3, tail()).tail.head shouldBeEqualTo 3
        permutationOf(4, 5, 6, tail()).tail.head shouldBeEqualTo 5
        concat(listOf(7, 8, 9, 10), tail()).tail.head shouldBeEqualTo 8
    }

    @Test
    fun `head 요청 시 tail을 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val lazy = cons(1) {
            tailEvaluated = true
            emptyPermutation()
        }
        lazy.head
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `tail의 head 요청 시 tail을 한 번만 평가해야 한다`() {
        var evalCount = 0
        val lazy = cons(1) {
            evalCount++
            cons(2) { emptyPermutation() }
        }
        val tailsHead = lazy.tail.head

        evalCount shouldBeEqualTo 1
        tailsHead shouldBeEqualTo 2
    }

    @Test
    fun `세 번째 요소 접근`() {
        val lazy = cons(1) {
            cons(2) {
                cons(3) { emptyPermutation() }
            }
        }
        lazy.tail.tail.head shouldBeEqualTo 3
    }

    @Test
    fun `끝을 넘어서 head 접근 시 예외 발생`() {
        val twoItems = permutationOf(1, 2)
        val tail = twoItems.tail.tail

        assertThrows<NoSuchElementException> {
            tail.head
        }
    }

    private fun tail(): () -> Permutation<Int> = { permutationOf(42) }
}
