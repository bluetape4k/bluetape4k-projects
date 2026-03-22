package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * equals/hashCode 계약 테스트
 */
class EqualsHashcodeTest: AbstractPermutationTest() {

    @Test
    fun `두 빈 순열은 동일하다`() {
        val first = emptyPermutation<Any>()
        val second = emptyPermutation<Any>()

        first.equals(second).shouldBeTrue()
        (first.hashCode() == second.hashCode()).shouldBeTrue()
    }

    @Test
    fun `빈 순열과 비어있지 않은 순열은 다르다`() {
        val first = permutationOf<Int>()
        val second = permutationOf(1)

        first.equals(second).shouldBeFalse()
        second.equals(first).shouldBeFalse()
    }

    @Test
    fun `단일 요소 순열 비교`() {
        val first = permutationOf(1)
        val second = permutationOf(1)
        val third = permutationOf(2)

        (first == second).shouldBeTrue()
        (first == third).shouldBeFalse()
        (second == third).shouldBeFalse()
    }

    @Test
    fun `고정 순열과 지연 유한 순열 비교`() {
        val first = permutationOf(1, 2, 3, 4)
        val second = cons(1) { cons(2) { cons(3) { permutationOf(4) } } }

        (first == second).shouldBeTrue()
        (first.hashCode() == second.hashCode()).shouldBeTrue()
    }

    @Test
    fun `두 지연 유한 순열 비교`() {
        val first = lazy()
        val second = lazy()

        (first == second).shouldBeTrue()
        (first.hashCode() == second.hashCode()).shouldBeTrue()
    }

    @Test
    fun `유한 순열과 무한 순열 비교`() {
        val first = permutationOf(2, 3, 5, 7)
        val second = primes()
        val third = primes().take(4)

        (first == second).shouldBeFalse()
        (first == third).shouldBeTrue()
    }

    @Test
    fun `빈 순열과 무한 순열 비교`() {
        val first = emptyPermutation<Int>()
        val second = primes()

        first.equals(second).shouldBeFalse()
        second.equals(first).shouldBeFalse()
    }
}
