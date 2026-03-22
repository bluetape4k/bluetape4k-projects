package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * iterator 테스트
 */
class IteratorTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 iterator는 hasNext가 false`() {
        emptyPermutation<Any>().iterator().hasNext().shouldBeFalse()
    }

    @Test
    fun `빈 iterator에서 hasNext 연속 호출 가능`() {
        val iterator = emptyPermutation<Any>().iterator()
        iterator.hasNext()
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `단일 요소 순열의 iterator 진행`() {
        val iterator = permutationOf("a").iterator()
        iterator.next() shouldBeEqualTo "a"
        iterator.hasNext().shouldBeFalse()

        assertThrows<NoSuchElementException> {
            iterator.next()
        }
    }

    @Test
    fun `고정 순열의 비어있지 않은 iterator`() {
        val iterator = permutationOf("a", "b", "c").iterator()

        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo "a"
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo "b"
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo "c"
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `hasNext 호출 없이 순회 가능`() {
        val iterator = permutationOf("a", "b", "c").iterator()
        iterator.next() shouldBeEqualTo "a"
        iterator.next() shouldBeEqualTo "b"
        iterator.next() shouldBeEqualTo "c"
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `hasNext 여러 번 호출 가능`() {
        val iterator = permutationOf("a", "b", "c").iterator()
        iterator.hasNext().shouldBeTrue()
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo "a"
        iterator.next() shouldBeEqualTo "b"
        iterator.hasNext().shouldBeTrue()
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo "c"
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `지연 유한 순열의 iterator 순회`() {
        val iterator = lazy().iterator()
        expectedList.forEach { expected ->
            iterator.hasNext().shouldBeTrue()
            iterator.next() shouldBeEqualTo expected
        }
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `지연 유한 순열의 iterator를 hasNext 없이 순회`() {
        val iterator = lazy().iterator()
        expectedList.forEach { expected ->
            iterator.next() shouldBeEqualTo expected
        }
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `iterator 생성 시 tail을 평가하지 않아야 한다`() {
        var tailEvaluated = false
        val lazy = cons(1) {
            tailEvaluated = true
            emptyPermutation()
        }
        lazy.iterator()
        tailEvaluated.shouldBeFalse()
    }

    @Test
    fun `iterator 진행 시 tail을 한 번만 평가해야 한다`() {
        var tailEvalCount = 0
        val lazy = cons(1) {
            tailEvalCount++
            emptyPermutation()
        }
        lazy.iterator().next()
        tailEvalCount shouldBeEqualTo 1
    }

    @Test
    fun `무한 순열의 iterator 생성`() {
        val naturals = numbers(1)
        val iterator = naturals.iterator()

        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo 1
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo 2
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo 3
        iterator.next() shouldBeEqualTo 4
        iterator.next() shouldBeEqualTo 5
    }

    @Test
    fun `유한 순열의 for-each 순회`() {
        val fixed = permutationOf('a', 'b', 'c')
        val collected = mutableListOf<Char>()
        for (c in fixed) {
            collected.add(c)
        }
        collected shouldBeEqualTo listOf('a', 'b', 'c')
    }
}
