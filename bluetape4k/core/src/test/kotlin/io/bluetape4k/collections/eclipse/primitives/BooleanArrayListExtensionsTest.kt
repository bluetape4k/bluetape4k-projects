package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * BooleanArrayListExtensions의 확장 함수들을 테스트한다.
 *
 * @author debop
 */
class BooleanArrayListExtensionsTest: AbstractCollectionTest() {

    companion object: KLogging()

    /**
     * BooleanArray.toBooleanArrayList 확장 함수 테스트
     */
    @Test
    fun `BooleanArray를 BooleanArrayList로 변환한다`() {
        val array = booleanArrayOf(true, false, true)
        val list = array.toBooleanArrayList()
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf(true, false, true)
    }

    /**
     * Iterable<Boolean>.toBooleanArrayList 확장 함수 테스트
     */
    @Test
    fun `Iterable을 BooleanArrayList로 변환한다`() {
        val iterable = listOf(true, false, false)
        val list = iterable.toBooleanArrayList()
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf(true, false, false)
    }

    /**
     * Sequence<Boolean>.toBooleanArrayList 확장 함수 테스트
     */
    @Test
    fun `Sequence를 BooleanArrayList로 변환한다`() {
        val sequence = sequenceOf(false, true)
        val list = sequence.toBooleanArrayList()
        list.size() shouldBeEqualTo 2
        list.asList() shouldBeEqualTo listOf(false, true)
    }

    /**
     * booleanArrayList 확장 함수 테스트
     */
    @Test
    fun `초기화 함수로 BooleanArrayList 생성`() {
        val list = booleanArrayList(5) { it % 2 == 0 }
        list.size() shouldBeEqualTo 5
        list.asList() shouldBeEqualTo listOf(true, false, true, false, true)
    }

    /**
     * booleanArrayListOf 확장 함수 테스트
     */
    @Test
    fun `vararg로 BooleanArrayList 생성`() {
        val list = booleanArrayListOf(true, false, true)
        list.size() shouldBeEqualTo 3
        list.asList() shouldBeEqualTo listOf(true, false, true)
    }

    /**
     * BooleanIterable의 asList, asSet 등 확장 함수 테스트
     */
    @Test
    fun `BooleanIterable 확장 함수들 테스트`() {
        val list = booleanArrayListOf(true, false, true)

        list.asList() shouldBeEqualTo listOf(true, false, true)
        list.asSet() shouldBeEqualTo setOf(true, false)

        list.asMutableList() shouldBeEqualTo listOf(true, false, true)
        list.asMutableSet() shouldBeEqualTo setOf(true, false)

        list.asFastList() shouldBeEqualTo fastListOf(true, false, true)
        list.asUnifiedSet() shouldBeEqualTo unifiedSetOf(true, false, true)
    }
}
