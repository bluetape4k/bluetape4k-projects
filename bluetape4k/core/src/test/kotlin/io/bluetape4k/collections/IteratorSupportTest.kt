package io.bluetape4k.collections

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IteratorSupportTest {

    @Test
    fun `asMutableIterator delegates next and hasNext`() {
        val iterator = listOf(1, 2, 3).iterator().asMutableIterator()
        iterator.hasNext().shouldBeTrue()
        iterator.next() shouldBeEqualTo 1
        iterator.next() shouldBeEqualTo 2
        iterator.next() shouldBeEqualTo 3
        iterator.hasNext().shouldBeFalse()
    }

    @Test
    fun `asMutableIterator remove throws`() {
        val iterator = listOf(1).iterator().asMutableIterator()
        assertThrows<UnsupportedOperationException> {
            iterator.remove()
        }
    }
}
