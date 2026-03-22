package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 인덱스 접근 테스트
 */
class GetTest: AbstractPermutationTest() {

    @Test
    fun `음수 인덱스로 접근 시 예외 발생`() {
        val naturals = numbers(1)
        assertThrows<IndexOutOfBoundsException> {
            naturals.get(-1)
        }
    }

    @Test
    fun `빈 순열의 첫 요소 접근 시 예외 발생`() {
        val empty = emptyPermutation<Int>()
        assertThrows<IndexOutOfBoundsException> {
            empty.get(0)
        }
    }

    @Test
    fun `범위를 넘어서는 접근 시 예외 발생`() {
        val seq = permutationOf(1, 2, 3)
        assertThrows<IndexOutOfBoundsException> {
            seq.get(3)
        }
    }

    @Test
    fun `첫 요소 접근 시 head 반환`() {
        permutationOf("a")[0] shouldBeEqualTo "a"
        permutationOf("b", "c", "d")[0] shouldBeEqualTo "b"
        cons("w") { emptyPermutation() }.head shouldBeEqualTo "w"
    }

    @Test
    fun `고정 순열의 마지막 요소 접근`() {
        permutationOf('a')[0] shouldBeEqualTo 'a'
        permutationOf('a', 'b')[1] shouldBeEqualTo 'b'
        permutationOf('a', 'b', 'c')[2] shouldBeEqualTo 'c'
    }

    @Test
    fun `먼 인덱스의 요소 접근`() {
        val naturals = numbers(0)
        naturals[100000] shouldBeEqualTo 100000
    }
}
