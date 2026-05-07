package io.bluetape4k.assertions.internal

import org.junit.jupiter.api.Test

class MessagesTest {

    @Test
    fun `stringify null returns null placeholder`() {
        assert(Messages.stringify(null) == "<null>")
    }

    @Test
    fun `stringify String wraps in quotes`() {
        assert(Messages.stringify("hello") == "\"hello\"")
    }

    @Test
    fun `stringify Collection formats elements`() {
        val result = Messages.stringify(listOf(1, 2, 3))
        assert(result == "[<1>, <2>, <3>]")
    }

    @Test
    fun `stringify Map formats entries`() {
        val result = Messages.stringify(mapOf("k" to "v"))
        assert(result == "{\"k\"=\"v\"}")
    }

    @Test
    fun `stringify IntArray formats elements`() {
        val result = Messages.stringify(intArrayOf(1, 2, 3))
        assert(result == "[1, 2, 3]")
    }

    @Test
    fun `stringify Throwable includes class name and message`() {
        val ex = IllegalStateException("boom")
        val result = Messages.stringify(ex)
        assert(result.contains("IllegalStateException"))
        assert(result.contains("boom"))
    }

    @Test
    fun `expectedToBe formats standard failure message`() {
        val msg = Messages.expectedToBe("equal to", "hello", "world")
        assert(msg.contains("equal to"))
        assert(msg.contains("hello"))
        assert(msg.contains("world"))
    }

    @Test
    fun `expectedNotToBe formats negation failure message`() {
        val msg = Messages.expectedNotToBe("be null", null, "value")
        assert(msg.contains("not to be null"))
    }
}
