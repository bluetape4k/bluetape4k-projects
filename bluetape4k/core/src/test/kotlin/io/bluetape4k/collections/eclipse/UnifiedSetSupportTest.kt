package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import org.junit.jupiter.api.Test

class UnifiedSetSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    @Test
    fun `빈 UnifiedSet 생성`() {
        val emptySet = emptyUnifiedSet<String>()

        emptySet.isEmpty.shouldBeTrue()
        emptySet.size shouldBeEqualTo 0
    }

    @Test
    fun `UnifiedSet 초기화`() {
        val set = unifiedSet(3) { 'a' + it }
        verifyUnifiedSet(set)

        verifyUnifiedSet(unifiedSetOf('a', 'b', 'c'))
    }

    private fun verifyUnifiedSet(set: UnifiedSet<Char>) {
        set.size shouldBeEqualTo 3
        set shouldBeEqualTo unifiedSetOf('a', 'b', 'c')
    }

    @Test
    fun `Collection 들을 UnifiedSet로 변환`() {
        val expectedSet = unifiedSetOf('a', 'b', 'c')

        listOf('a', 'b', 'c').toUnifiedSet() shouldBeEqualTo expectedSet
        setOf('a', 'b', 'c').toUnifiedSet() shouldBeEqualTo expectedSet
        ('a'..'c').toUnifiedSet() shouldBeEqualTo expectedSet
        ('a'..'c').asSequence().toUnifiedSet() shouldBeEqualTo expectedSet
        ('a'..'c').iterator().toUnifiedSet() shouldBeEqualTo expectedSet
        arrayOf('a', 'b', 'c').toUnifiedSet() shouldBeEqualTo expectedSet
    }
}
