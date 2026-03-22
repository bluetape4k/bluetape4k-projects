package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * drop(n) 테스트
 */
class DropTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열에서 음수 drop 시 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            emptyPermutation<Any>().drop(-1L)
        }
    }

    @Test
    fun `비어있지 않은 순열에서 음수 drop 시 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            permutationOf(1, 2, 3).drop(-1L)
        }
    }

    @Test
    fun `무한 순열에서 음수 drop 시 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            primes().drop(-1L)
        }
    }

    @Test
    fun `빈 순열 반환`() {
        emptyPermutation<Any>().drop(0).isEmpty().shouldBeTrue()
        emptyPermutation<Any>().drop(5).isEmpty().shouldBeTrue()
        permutationOf(1, 2).drop(2).isEmpty().shouldBeTrue()
    }

    @Test
    fun `접두사를 제외한 부분 순열 반환`() {
        permutationOf(1, 2, 3, 4, 5).drop(2) shouldBeEqualTo permutationOf(3, 4, 5)

        val naturals = numbers(0)
        val fromFive = naturals.drop(5)
        fromFive.take(4) shouldBeEqualTo permutationOf(5, 6, 7, 8)
    }

    @Test
    fun `drop 시 필요한 만큼만 tail을 평가한다`() {
        var count = 0
        val seq = cons(1) {
            count++
            cons(2) {
                count++
                cons(3) { emptyPermutation() }
            }
        }
        seq.drop(2)
        count shouldBeEqualTo 2
    }
}
