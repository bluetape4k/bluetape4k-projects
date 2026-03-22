package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.stream.Stream

/**
 * Stream 변환 테스트
 */
class StreamTest: AbstractPermutationTest() {

    private val expectedElements = listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    private val expectedElementAsString = listOf("5", "6", "7", "8", "9", "10", "11", "12", "13", "14")

    private fun stackedStream(permutation: Permutation<Int>): Stream<Int> {
        return permutation.toStream()
            .map { it + 1 }
            .flatMap { listOf(0, it - 1).stream() }
            .filter { it != 0 }
            .skip(4)
            .limit(10)
            .sorted()
            .distinct()
    }

    @Test
    fun `여러 연산을 체이닝`() {
        val numbers = numbers(1).take(20)
        val collected: Permutation<Int> = stackedStream(numbers).toPermutation()
        collected shouldBeEqualTo permutationOf(expectedElements)
    }

    @Test
    fun `여러 연산 체이닝 후 리스트로 변환`() {
        val numbers = numbers(1).take(20)
        val collected = stackedStream(numbers).toList()
        collected shouldBeEqualTo permutationOf(expectedElements)
    }

    @Test
    fun `여러 연산 체이닝 후 커스텀 collector 사용`() {
        val numbers = numbers(1).take(20)
        val intStream = stackedStream(numbers)

        val collected = intStream.collect(
            { ArrayList<String>() },
            { list, item -> list.add(item.toString()) },
            { list, items -> list.addAll(items) },
        )
        collected shouldBeEqualTo permutationOf(expectedElementAsString)
    }

    @Test
    fun `종단 연산 실행`() {
        val numbers = numbers(1).take(20)
        val min = stackedStream(numbers).min { a, b -> a - b }
        min shouldBeEqualTo Optional.of(expectedElements.min())
    }
}
