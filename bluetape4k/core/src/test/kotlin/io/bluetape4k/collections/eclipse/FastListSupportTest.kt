package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class FastListSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    val expectedList = fastList(10) { it + 1 }

    @Test
    fun `빈 FastList 생성`() {
        val emptyList = emptyFastList<String>()

        emptyList.isEmpty.shouldBeTrue()
        emptyList.size shouldBeEqualTo 0
    }

    @Test
    fun `초기 size가 음수일때는 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            fastList(-1) { it.toString() }
        }
    }

    @Test
    fun `초기화를 이용하여 FastList 생성하기`() {
        val list = fastList(10) { it }
        list.size shouldBeEqualTo 10
        list shouldBeEqualTo fastListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    }

    @Test
    fun `Collection 들을 FastList로 변환`() {
        (1..10).toFastList() shouldBeEqualTo expectedList
        (1..10).asSequence().toFastList() shouldBeEqualTo expectedList
        (1..10).iterator().toFastList() shouldBeEqualTo expectedList
        (1..10).toList().toTypedArray().toFastList() shouldBeEqualTo expectedList
    }

    /**
     * FastListSupport 확장 함수들의 동작을 검증하는 테스트를 추가합니다.
     */
    @Test
    fun `FastList에 요소 추가 및 삭제 동작 확인`() {
        val list = fastListOf("A", "B", "C")
        list.add("D")
        list.remove("B")
        list shouldBeEqualTo fastListOf("A", "C", "D")
    }

    @Test
    fun `FastList에 null 요소 허용 여부 확인`() {
        val list = fastListOf(null, "A", null)
        list.size shouldBeEqualTo 3
        list[0] shouldBeEqualTo null
        list[1] shouldBeEqualTo "A"
        list[2] shouldBeEqualTo null
    }

    @Test
    fun `FastList 확장 toFastList가 FastList를 반환하는지 확인`() {
        val original = fastListOf(1, 2, 3)
        val converted = original.toFastList()
        converted shouldBeEqualTo original
    }

    @Test
    fun `FastList 확장 toFastList가 Iterable을 FastList로 변환하는지 확인`() {
        val iterable = listOf(1, 2, 3)
        val fastList = iterable.toFastList()
        fastList shouldBeEqualTo fastListOf(1, 2, 3)
    }

    @Test
    fun `FastList 확장 toFastList가 Sequence를 FastList로 변환하는지 확인`() {
        val sequence = sequenceOf(1, 2, 3)
        val fastList = sequence.toFastList()
        fastList shouldBeEqualTo fastListOf(1, 2, 3)
    }

    @Test
    fun `FastList 확장 toFastList가 Iterator를 FastList로 변환하는지 확인`() {
        val iterator = listOf(1, 2, 3).iterator()
        val fastList = iterator.toFastList()
        fastList shouldBeEqualTo fastListOf(1, 2, 3)
    }

    @Test
    fun `FastList 확장 toFastList가 Array를 FastList로 변환하는지 확인`() {
        val array = arrayOf(1, 2, 3)
        val fastList = array.toFastList()
        fastList shouldBeEqualTo fastListOf(1, 2, 3)
    }
}
