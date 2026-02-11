package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ObjectSupportTest {

    companion object: KLogging()

    @Test
    fun `요소 중 가장 빈번히 나오는 값을 찾습니다`() {
        modeOrNull(1, 2, 2, 3, 3, 3, 4, 5) shouldBeEqualTo 3
        modeOrNull(1, 2, 2, 3, 3, 4, 5).shouldBeNull()
        modeOrNull(1, 2, 3, 4, 5).shouldBeNull()
    }

    @Test
    fun `Iterable의 요소 중 가장 빈번히 나오는 값을 찾습니다`() {
        listOf(1, 2, 2, 3, 3, 3, 4, 5).modeOrNull() shouldBeEqualTo 3
        listOf(1, 2, 2, 3, 3, 4, 5).modeOrNull().shouldBeNull()
        listOf(1, 2, 3, 4, 5).modeOrNull().shouldBeNull()

        // 빈 컬렉션
        emptyList<Int>().modeOrNull().shouldBeNull()

        // 단일 요소
        listOf(42).modeOrNull() shouldBeEqualTo 42

        // 문자열 타입
        listOf("a", "b", "b", "c").modeOrNull() shouldBeEqualTo "b"
        listOf("a", "a", "b", "b").modeOrNull().shouldBeNull()
    }

    @Test
    fun `Sequence의 요소 중 가장 빈번히 나오는 값을 찾습니다`() {
        sequenceOf(1, 2, 2, 3, 3, 3, 4, 5).modeOrNull() shouldBeEqualTo 3
        sequenceOf(1, 2, 2, 3, 3, 4, 5).modeOrNull().shouldBeNull()
        sequenceOf(1, 2, 3, 4, 5).modeOrNull().shouldBeNull()

        // 빈 시퀀스
        emptySequence<Int>().modeOrNull().shouldBeNull()
    }

    @Test
    fun `Comparable 타입의 컬렉션에서 중간 값을 찾습니다`() {
        listOf(1, 2, 3, 4, 5).median() shouldBeEqualTo 3
        listOf(1, 1, 1).median() shouldBeEqualTo 1

        // 짝수개일 때 작은 값 반환
        listOf(1, 2, 3, 4).median() shouldBeEqualTo 2
        listOf(1, 2, 3, 4, 5, 6).median() shouldBeEqualTo 3

        // 중복 요소
        listOf(1, 1, 1, 2, 3).median() shouldBeEqualTo 1

        // 단일 요소
        listOf(42).median() shouldBeEqualTo 42

        // 문자열
        listOf("a", "b", "c").median() shouldBeEqualTo "b"
    }

    @Test
    fun `빈 컬렉션의 median은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            emptyList<Int>().median()
        }
        assertFailsWith<IllegalArgumentException> {
            emptyList<Int>().median(Comparator.naturalOrder())
        }
    }

    @Test
    fun `컬렉션의 중간 값을 Comparator로 찾습니다`() {
        val ordinal = Comparator.naturalOrder<Int>()

        listOf(1, 2, 3, 4, 5).median(ordinal) shouldBeEqualTo 3
        listOf(1, 1, 1).median(ordinal) shouldBeEqualTo 1

        // 중간 값이 2개일 때는 작은 값을 반환합니다.
        listOf(1, 2, 3, 4).median(ordinal) shouldBeEqualTo 2
        listOf(1, 2, 3, 4, 5, 6).median(ordinal) shouldBeEqualTo 3

        // 중복 요소가 있는 경우에도 정확히 중간 값을 반환합니다.
        listOf(1, 1, 1, 2, 3).median(ordinal) shouldBeEqualTo 1
        listOf(3, 3, 3, 3, 5).median(ordinal) shouldBeEqualTo 3
    }

    @Test
    fun `컬렉션의 중간값을 Custom Comparator로 찾습니다`() {
        // reverse order
        val reverse = Comparator.reverseOrder<Int>()

        listOf(1, 2, 3, 4, 5).median(reverse) shouldBeEqualTo 3
        listOf(1, 1, 1).median(reverse) shouldBeEqualTo 1

        // 중간 값이 2개일 때는 작은 값을 반환합니다.
        listOf(1, 2, 3, 4).median(reverse) shouldBeEqualTo 3
        listOf(1, 2, 3, 4, 5, 6).median(reverse) shouldBeEqualTo 4

        // 중복 요소
        listOf(1, 1, 1, 2, 3).median(reverse) shouldBeEqualTo 1
    }
}
