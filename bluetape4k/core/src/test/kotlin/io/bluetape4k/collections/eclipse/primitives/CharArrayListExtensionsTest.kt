package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CharArrayListExtensionsTest: AbstractCollectionTest() {

    companion object: KLogging()

    /**
     * CharArray.toCharArrayList 확장 함수 테스트
     */
    @Test
    fun `CharArray를 CharArrayList로 변환한다`() {
        val array = charArrayOf('a', 'b', 'c')
        val list = array.toCharArrayList()
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf('a', 'b', 'c')
    }

    /**
     * Iterable<Char>.toCharArrayList 확장 함수 테스트
     */
    @Test
    fun `Iterable을 CharArrayList로 변환한다`() {
        val iterable = listOf('x', 'y', 'z')
        val list = iterable.toCharArrayList()
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf('x', 'y', 'z')
    }

    /**
     * Sequence<Char>.toCharArrayList 확장 함수 테스트
     */
    @Test
    fun `Sequence를 CharArrayList로 변환한다`() {
        val sequence = sequenceOf('1', '2')
        val list = sequence.toCharArrayList()
        list.size() shouldBeEqualTo 2
        list.asList() shouldBeEqualTo listOf('1', '2')
    }

    /**
     * charArrayList 확장 함수 테스트
     */
    @Test
    fun `초기화 함수로 CharArrayList 생성`() {
        val list = charArrayList(5) { ('a'.code + it).toChar() }
        list.size() shouldBeEqualTo 5
        list.asList() shouldBeEqualTo listOf('a', 'b', 'c', 'd', 'e')
    }

    /**
     * charArrayListOf 확장 함수 테스트
     */
    @Test
    fun `vararg로 CharArrayList 생성`() {
        val list = charArrayListOf('x', 'y', 'z')
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf('x', 'y', 'z')
    }

    /**
     * Iterable<Any>.asCharArrayList 확장 함수 테스트
     */
    @Test
    fun `Any Iterable을 CharArrayList로 변환한다`() {
        val iterable = listOf(65, 66, 67)
        val list = iterable.asCharArrayList()
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf('A', 'B', 'C')
    }

    /**
     * CharIterable의 asList, asSet 등 확장 함수 테스트
     */
    @Test
    fun `CharIterable 확장 함수들 테스트`() {
        val list = charArrayListOf('a', 'b', 'a')

        list.asList() shouldBeEqualTo listOf('a', 'b', 'a')
        list.asSet() shouldBeEqualTo setOf('a', 'b')

        list.asMutableList() shouldBeEqualTo listOf('a', 'b', 'a')
        list.asMutableSet() shouldBeEqualTo setOf('a', 'b')

        list.toFastList() shouldBeEqualTo fastListOf('a', 'b', 'a')
        list.toUnifiedSet() shouldBeEqualTo unifiedSetOf('a', 'b', 'a')
    }

    /**
     * CharIterable의 maxOrNull, minOrNull 확장 함수 테스트
     */
    @Test
    fun `CharIterable의 maxOrNull, minOrNull 테스트`() {
        val list = charArrayListOf('d', 'a', 'c')
        list.maxOrNull() shouldBeEqualTo 'd'
        list.minOrNull() shouldBeEqualTo 'a'

        val emptyList = charArrayListOf()
        emptyList.maxOrNull() shouldBeEqualTo null
        emptyList.minOrNull() shouldBeEqualTo null
    }
}
