package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * zip 두 순열 테스트
 */
class ZipTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열끼리 zip`() {
        emptyIntPermutation.zip(emptyIntPermutation) { a, b -> a + b }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `빈 순열과 비어있지 않은 순열 zip`() {
        emptyIntPermutation.zip(permutationOf(1)) { a, b -> a + b }.isEmpty().shouldBeTrue()
        permutationOf(1).zip(emptyIntPermutation) { a, b -> a + b }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `같은 크기의 유한 순열 zip`() {
        val first = permutationOf("A", "B", "C")
        val second = permutationOf(1, 2, 3)
        val zipped = first.zip(second) { s, i -> s + i.toString() }
        zipped shouldBeEqualTo permutationOf("A1", "B2", "C3")
    }

    @Test
    fun `같은 크기의 지연 순열 zip`() {
        val first = cons("A") { cons("B") { permutationOf("C") } }
        val second = cons(1) { cons(2) { permutationOf(3) } }
        val zipped = first.zip(second) { s, i -> s + i.toString() }
        zipped shouldBeEqualTo permutationOf("A1", "B2", "C3")
    }

    @Test
    fun `다른 크기의 유한 순열 zip - 첫번째가 짧은 경우`() {
        val first = permutationOf("A", "B", "C")
        val second = permutationOf(1, 2, 3, 4)
        val zipped = first.zip(second) { s, i -> s + i.toString() }
        zipped shouldBeEqualTo permutationOf("A1", "B2", "C3")
    }

    @Test
    fun `다른 크기의 유한 순열 zip - 두번째가 짧은 경우`() {
        val first = permutationOf("A", "B", "C", "D")
        val second = permutationOf(1, 2, 3)
        val zipped = first.zip(second) { s, i -> s + i.toString() }
        zipped shouldBeEqualTo permutationOf("A1", "B2", "C3")
    }

    @Test
    fun `두 무한 순열 zip`() {
        val naturals = numbers(1)
        val primeSeq = primes()
        val zipped = naturals.zip(primeSeq) { n, p -> "$n:$p" }
        zipped.take(5) shouldBeEqualTo permutationOf("1:2", "2:3", "3:5", "4:7", "5:11")

        val fixedPrimes = primes().take(5)
        val zippedFixed = naturals.zip(fixedPrimes) { n, p -> "$n:$p" }
        zippedFixed shouldBeEqualTo permutationOf("1:2", "2:3", "3:5", "4:7", "5:11")
    }

    @Test
    fun `zip 시 tail을 평가하지 않아야 한다`() {
        var firstTailEvaluated = false
        var secondTailEvaluated = false
        val first = cons("A") {
            firstTailEvaluated = true
            emptyPermutation()
        }
        val second = cons(1) {
            secondTailEvaluated = true
            emptyPermutation()
        }
        first.zip(second) { c, i -> c + i }

        firstTailEvaluated.shouldBeFalse()
        secondTailEvaluated.shouldBeFalse()
    }
}
