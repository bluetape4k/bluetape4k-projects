package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class FixedListSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    val expectedList = fixedSizeList(10) { it + 1 }

    @Test
    fun `빈 FixedSizeList 생성`() {
        val emptyList = emptyFixedSizeList<String>()

        emptyList.isEmpty.shouldBeTrue()
        emptyList.size shouldBeEqualTo 0
    }

    @Test
    fun `초기 size가 음수일때는 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            fixedSizeList(-1) { it.toString() }
        }
    }

    @Test
    fun `초기화를 이용하여 FixedSizeList 생성하기`() {
        val list = fixedSizeList(10) { it }
        list.size shouldBeEqualTo 10
        list shouldBeEqualTo fixedSizeListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    }

    @Test
    fun `Collection 들을 FixedSizeList로 변환`() {
        (1..10).toFixedSizeList() shouldBeEqualTo expectedList
        (1..10).asSequence().toFixedSizeList() shouldBeEqualTo expectedList
        (1..10).iterator().toFixedSizeList() shouldBeEqualTo expectedList
        (1..10).toList().toTypedArray().toFixedSizeList() shouldBeEqualTo expectedList
    }

    @Test
    fun `FixedSizeList 확장 toFixedSizeList가 FixedSizeList를 반환하는지 확인`() {
        val original = fixedSizeListOf(1, 2, 3)
        val converted = original.toFixedSizeList()
        converted shouldBeEqualTo original
    }

    @Test
    fun `FixedSizeList 확장 toFixedSizeList가 Iterable을 FixedSizeList로 변환하는지 확인`() {
        val iterable = listOf(1, 2, 3)
        val fixedSizeList = iterable.toFixedSizeList()
        fixedSizeList shouldBeEqualTo fixedSizeListOf(1, 2, 3)
    }

    @Test
    fun `FixedSizeList 확장 toFixedSizeList가 Sequence를 FixedSizeList로 변환하는지 확인`() {
        val sequence = sequenceOf(1, 2, 3)
        val fixedSizeList = sequence.toFixedSizeList()
        fixedSizeList shouldBeEqualTo fixedSizeListOf(1, 2, 3)
    }

    @Test
    fun `FixedSizeList 확장 toFixedSizeList가 Iterator를 FixedSizeList로 변환하는지 확인`() {
        val iterator = listOf(1, 2, 3).iterator()
        val fixedSizeList = iterator.toFixedSizeList()
        fixedSizeList shouldBeEqualTo fixedSizeListOf(1, 2, 3)
    }

    @Test
    fun `FixedSizeList 확장 toFixedSizeList가 Array를 FixedSizeList로 변환하는지 확인`() {
        val array = arrayOf(1, 2, 3)
        val fixedSizeList = array.toFixedSizeList()
        fixedSizeList shouldBeEqualTo fixedSizeListOf(1, 2, 3)
    }
}
