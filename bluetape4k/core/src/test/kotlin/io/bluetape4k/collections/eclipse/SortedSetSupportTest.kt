package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.eclipse.collections.api.set.sorted.MutableSortedSet
import org.junit.jupiter.api.Test

class SortedSetSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    @Test
    fun `빈 ImmutableSortedSet 생성`() {
        val emptySet = emptyImmutableSortedSet<String>()

        emptySet.isEmpty.shouldBeTrue()
        emptySet.size() shouldBeEqualTo 0
    }

    @Test
    fun `빈 MutableSortedSet 생성`() {
        val emptySet = emptyMutableSortedSet<String>()

        emptySet.isEmpty.shouldBeTrue()
        emptySet.size shouldBeEqualTo 0
    }

    @Test
    fun `MutableSortedSet 초기화`() {
        val set = mutableSortedSet(3) { 'a' + it }
        verifyUnifiedSet(set)

        verifyUnifiedSet(mutableSortedSetOf('a', 'b', 'c'))
    }

    private fun verifyUnifiedSet(set: MutableSortedSet<Char>) {
        set.size shouldBeEqualTo 3
        set shouldBeEqualTo immutableSortedSetOf('a', 'b', 'c')
    }

    @Test
    fun `Collection 들을 MutableSortedSet으로 변환`() {
        val expectedSet = immutableSortedSetOf('a', 'b', 'c')

        listOf('a', 'b', 'c').toMutableSortedSet() shouldBeEqualTo expectedSet
        setOf('a', 'b', 'c').toMutableSortedSet() shouldBeEqualTo expectedSet
        ('a'..'c').toMutableSortedSet() shouldBeEqualTo expectedSet
        ('a'..'c').asSequence().toMutableSortedSet() shouldBeEqualTo expectedSet
        ('a'..'c').iterator().toMutableSortedSet() shouldBeEqualTo expectedSet
        arrayOf('a', 'b', 'c').toMutableSortedSet() shouldBeEqualTo expectedSet
    }
}
