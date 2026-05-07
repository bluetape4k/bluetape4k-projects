package io.bluetape4k.collections.permutations

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.Collections.emptyIterator

/**
 * 생성 테스트 (permutationOf, cons, iterate, tabulate, continually)
 */
class BuildingTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열 생성`() {
        emptyPermutation<Any>().isEmpty().shouldBeTrue()
        emptyPermutation<Any>().size shouldBeEqualTo 0
    }

    @Test
    fun `빈 가변인자 순열 생성`() {
        permutationOf<Any>().isEmpty().shouldBeTrue()
        permutationOf<Any>().size shouldBeEqualTo 0
    }

    @Test
    fun `빈 컬렉션으로 순열 생성`() {
        permutationOf<Any>(emptyList()).isEmpty().shouldBeTrue()
        permutationOf<Any>(emptyList()).size shouldBeEqualTo 0
    }

    @Test
    fun `빈 Iterator로 순열 생성`() {
        permutationOf<Any>(emptyIterator()).isEmpty().shouldBeTrue()
        permutationOf<Any>(emptyIterator()).size shouldBeEqualTo 0
    }

    @Test
    fun `하나의 요소로 순열 생성`() {
        permutationOf(1).size shouldBeEqualTo 1
        permutationOf(1).isEmpty().shouldBeFalse()
    }

    @Test
    fun `두 요소로 순열 생성`() {
        permutationOf(2, 3).size shouldBeEqualTo 2
        permutationOf(2, 3).isEmpty().shouldBeFalse()
    }

    @Test
    fun `세 요소로 순열 생성`() {
        permutationOf(4, 5, 6).size shouldBeEqualTo 3
        permutationOf(4, 5, 6).isEmpty().shouldBeFalse()
    }

    @Test
    fun `여러 요소로 순열 생성`() {
        permutationOf(7, 8, 9, 1, 2, 3, 4, 5, 6).size shouldBeEqualTo 9
        permutationOf(7, 8, 9, 1, 2, 3, 4, 5, 6).isEmpty().shouldBeFalse()
    }

    @Test
    fun `Iterable로 순열 생성`() {
        permutationOf(listOf(7, 8, 9, 1, 2, 3, 4, 5, 6)).size shouldBeEqualTo 9
        permutationOf(listOf(7, 8, 9, 1, 2, 3, 4, 5, 6)).isEmpty().shouldBeFalse()
    }

    @Test
    fun `Iterator로 순열 생성`() {
        permutationOf(listOf(7, 8, 9, 1, 2, 3, 4, 5, 6).iterator()).size shouldBeEqualTo 9
        permutationOf(listOf(7, 8, 9, 1, 2, 3, 4, 5, 6).iterator()).isEmpty().shouldBeFalse()
    }

    @Test
    fun `고정 요소로 시작하는 무한 순열 생성`() {
        val infinite = permutationOf(1, 2, 3) { numbers(4) }
        infinite.take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `Iterable과 tail 함수로 무한 순열 생성`() {
        val infinite = concat(listOf(1, 2, 3)) { numbers(4) }
        infinite.take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `Iterator와 tail 함수로 무한 순열 생성`() {
        val infinite = concat(listOf(1, 2, 3).iterator()) { numbers(4) }
        infinite.take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `cons와 supplier로 무한 순열 생성`() {
        val infinite = cons(1) { numbers(2) }
        infinite.take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `cons와 구체적인 순열로 무한 순열 생성`() {
        val infinite = permutationOf(1) + numbers(2)
        infinite.take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }
}
