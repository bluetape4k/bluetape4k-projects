package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * forEach 순회 테스트
 */
class ForEachTest: AbstractPermutationTest() {

    @Test
    fun `빈 순열의 forEach는 아무것도 하지 않는다`() {
        val collected = mutableListOf<Int>()
        emptyPermutation<Int>().forEach { collected.add(it) }
        collected.isEmpty().shouldBeTrue()
    }

    @Test
    fun `단일 요소 순열에서 forEach 호출`() {
        val collected = mutableListOf<Int>()
        permutationOf(1).forEach { collected.add(it) }
        collected shouldBeEqualTo listOf(1)
    }

    @Test
    fun `여러 요소 고정 순열에서 forEach 호출`() {
        val collected = mutableListOf<Int>()
        permutationOf(2, 3, 4).forEach { collected.add(it) }
        collected shouldBeEqualTo listOf(2, 3, 4)
    }

    @Test
    fun `부분 순열에서 forEach 호출`() {
        val collected = mutableListOf<Int>()
        permutationOf(2, 3, 4, 5, 6, 7).take(3).forEach { collected.add(it) }
        collected shouldBeEqualTo listOf(2, 3, 4)
    }

    @Test
    fun `지연 생성된 유한 순열에서 forEach 호출`() {
        val collected = mutableListOf<Int>()
        val fixed = cons(5) { cons(6) { permutationOf(7) } }
        fixed.forEach { collected.add(it) }
        collected shouldBeEqualTo listOf(5, 6, 7)
    }
}
