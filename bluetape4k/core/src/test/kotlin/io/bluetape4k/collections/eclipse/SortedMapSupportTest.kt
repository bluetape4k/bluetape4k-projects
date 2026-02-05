package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.eclipse.collections.api.map.sorted.MutableSortedMap
import org.junit.jupiter.api.Test

class SortedMapSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    @Test
    fun `빈 Immutable Sorted Map 생성`() {
        val emptyMap = emptyImmutableSortedMap<String, Int>()

        emptyMap.isEmpty.shouldBeTrue()
        emptyMap.size() shouldBeEqualTo 0
    }

    @Test
    fun `빈 Mutable Sorted Map 생성`() {
        val emptyMap = emptyMutableSortedMap<String, Int>()

        emptyMap.isEmpty.shouldBeTrue()
        emptyMap.size shouldBeEqualTo 0
    }

    @Test
    fun `MutableSortedMap 생성`() {
        val map = mutableSortedMapOf(1 to 'a', 2 to 'b', 3 to 'c')
        map.verify()
    }

    @Test
    fun `capacity 를 사용하여 MutableSortedMap 초기화`() {
        val map = mutableSortedMap(3) { it + 1 to ('a' + it) }
        map.verify()
    }

    @Test
    fun `Collection 들을 MutableSortedMap 변환`() {
        mapOf(1 to 'a', 2 to 'b', 3 to 'c').toMutableSortedMap().verify()
        listOf(1 to 'a', 2 to 'b', 3 to 'c').toMutableSortedMap().verify()
        setOf(1 to 'a', 2 to 'b', 3 to 'c').toMutableSortedMap().verify()
        (1..3).map { it to ('a' + it - 1) }.toMutableSortedMap().verify()
        (1..3).asSequence().map { it to ('a' + it - 1) }.toMutableSortedMap().verify()
        (1..3).iterator().asSequence().map { it to ('a' + it - 1) }.toMutableSortedMap().verify()
        arrayOf(1 to 'a', 2 to 'b', 3 to 'c').toMutableSortedMap().verify()
    }

    @Test
    fun `pair 를 unifiedMap으로 변환`() {
        val map = fixedSizeListOf(1 to 'a', 2 to 'b', 3 to 'c').toMutableSortedMap()
        map.verify()
    }

    private fun MutableSortedMap<Int, Char>.verify() {
        this.size shouldBeEqualTo 3
        this[1] shouldBeEqualTo 'a'
        this[2] shouldBeEqualTo 'b'
        this[3] shouldBeEqualTo 'c'
    }
}
