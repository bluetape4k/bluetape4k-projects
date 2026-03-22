package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * tail 접근 테스트
 */
class TailTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 tail 접근 시 예외 발생`() {
        assertThrows<NoSuchElementException> {
            emptyPermutation.tail
        }
    }

    @Test
    fun `단일 요소 순열의 tail은 비어있다`() {
        permutationOf(1).tail.isEmpty().shouldBeTrue()
    }

    @Test
    fun `고정 순열의 tail 접근`() {
        permutationOf(1).tail.isEmpty().shouldBeTrue()
        permutationOf(2, 3).tail shouldBeEqualTo permutationOf(3)
        permutationOf(4, 5, 6).tail shouldBeEqualTo permutationOf(5, 6)
        permutationOf(7, 8, 9, 10).tail shouldBeEqualTo permutationOf(8, 9, 10)
    }

    @Test
    fun `무한 순열의 tail 접근`() {
        numbers(1).tail.limit(4) shouldBeEqualTo permutationOf(2, 3, 4, 5)
    }
}
