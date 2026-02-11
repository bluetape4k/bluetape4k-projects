package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class StringBuilderSupportTest {

    @Test
    fun `append iterable items`() {
        val list = listOf(1, 2, 3)
        val str = buildString {
            appendItems(list)
        }
        str shouldBeEqualTo "1, 2, 3"
    }

    @Test
    fun `append iterable items to exist string builder`() {
        val list = listOf(1, 2, 3)
        val builder = StringBuilder()

        list.appendItems(builder, ",")
        builder.toString() shouldBeEqualTo "1,2,3"
    }

    @Test
    fun `append sequence items`() {
        val seq = listOf(1, 2, 3)
        val str = buildString {
            appendItems(seq.asSequence())
        }
        str shouldBeEqualTo "1, 2, 3"
    }

    @Test
    fun `append sequence items to exist string builder`() {
        val seq = sequenceOf(1, 2, 3)
        val builder = StringBuilder()

        seq.appendItems(builder, ",")
        builder.toString() shouldBeEqualTo "1,2,3"
    }
}
