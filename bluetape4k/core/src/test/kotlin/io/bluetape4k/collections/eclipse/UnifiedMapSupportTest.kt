package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import org.junit.jupiter.api.Test

class UnifiedMapSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    @Test
    fun `빈 UnifiedMap 생성`() {
        val emptyMap = emptyUnifiedMap<String, Int>()

        emptyMap.isEmpty.shouldBeTrue()
        emptyMap.size() shouldBeEqualTo 0
    }

    @Test
    fun `UnifiedMap 생성`() {
        val map = unifiedMapOf(1 to 'a', 2 to 'b', 3 to 'c')
        map.verify()
    }

    @Test
    fun `capacity 사용하여 UnifiedMap 초기화`() {
        val map = UnifiedMap(3) { it + 1 to ('a' + it) }
        map.verify()
        unifiedMapOf(1 to 'a', 2 to 'b', 3 to 'c').verify()
    }

    @Test
    fun `Collection 들을 UnifiedMap로 변환`() {
        mapOf(1 to 'a', 2 to 'b', 3 to 'c').toUnifiedMap().verify()
        listOf(1 to 'a', 2 to 'b', 3 to 'c').toUnifiedMap().verify()
        setOf(1 to 'a', 2 to 'b', 3 to 'c').toUnifiedMap().verify()
        (1..3).map { it to ('a' + it - 1) }.toUnifiedMap().verify()
        (1..3).asSequence().map { it to ('a' + it - 1) }.toUnifiedMap().verify()
        (1..3).iterator().asSequence().map { it to ('a' + it - 1) }.toUnifiedMap().verify()
        arrayOf(1 to 'a', 2 to 'b', 3 to 'c').toUnifiedMap().verify()
    }


    @Test
    fun `pair 를 unifiedMap으로 변환`() {
        val pairs = listOf(1 to 'a', 2 to 'b', 3 to 'c')
        val map = pairs.toUnifiedMap()
        map.verify()
    }

    private fun UnifiedMap<Int, Char>.verify() {
        this.size shouldBeEqualTo 3
        this[1] shouldBeEqualTo 'a'
        this[2] shouldBeEqualTo 'b'
        this[3] shouldBeEqualTo 'c'
    }
}
