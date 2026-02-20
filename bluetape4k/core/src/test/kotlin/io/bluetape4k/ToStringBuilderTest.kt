package io.bluetape4k

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ToStringBuilderTest {

    companion object: KLogging()

    @Test
    fun `create with empty string`() {
        assertFailsWith<IllegalArgumentException> {
            ToStringBuilder("")
        }
    }

    @Test
    fun `create with blank string`() {
        assertFailsWith<IllegalArgumentException> {
            ToStringBuilder(" \t  ")
        }
    }

    @Test
    fun `create simple entity`() {
        val builder = ToStringBuilder.Companion("object").apply {
            add("a", 1)
            add("b", "two")
        }
        builder.toString() shouldBeEqualTo "object(a=1,b=two)"
    }

    @Test
    fun `create simple entity with null property`() {
        val builder = ToStringBuilder.Companion("object").apply {
            add("a", 1)
            add("b", "two")
            add("c", null)
        }
        builder.toString() shouldBeEqualTo "object(a=1,b=two,c=<null>)"
    }

    @Test
    fun `toString with limit does not pollute cached value`() {
        val builder = ToStringBuilder("object").apply {
            add("a", 1)
            add("b", "two")
            add("c", "three")
        }

        builder.toString(limit = 2) shouldBeEqualTo "object(a=1,b=two,...)"
        builder.toString() shouldBeEqualTo "object(a=1,b=two,c=three)"
    }

    @Test
    fun `add invalidates cached string`() {
        val builder = ToStringBuilder("object").apply {
            add("a", 1)
        }
        builder.toString() shouldBeEqualTo "object(a=1)"

        builder.add("b", "two")
        builder.toString() shouldBeEqualTo "object(a=1,b=two)"
    }
}
