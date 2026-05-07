package io.bluetape4k.collections

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

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
        assertFailsWith<UnsupportedOperationException> {
            iterator.remove()
        }
    }
}
