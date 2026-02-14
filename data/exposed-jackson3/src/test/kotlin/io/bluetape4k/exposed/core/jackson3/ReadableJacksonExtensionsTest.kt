package io.bluetape4k.exposed.core.jackson3

import io.r2dbc.spi.Readable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReadableJacksonExtensionsTest {

    private data class Payload(val name: String, val age: Int)

    private class FakeReadable(
        private val valuesByIndex: Map<Int, Any?> = emptyMap(),
        private val valuesByName: Map<String, Any?> = emptyMap(),
    ): Readable {
        override fun <T: Any?> get(index: Int, type: Class<T>): T? {
            val value = valuesByIndex[index] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun <T: Any?> get(name: String, type: Class<T>): T? {
            val value = valuesByName[name] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun get(index: Int): Any? = valuesByIndex[index]
        override fun get(name: String): Any? = valuesByName[name]
    }

    @Test
    fun `Readable jackson3 getter supports index and name`() {
        val jsonText = """{"name":"blue","age":20}"""
        val readable = FakeReadable(
            valuesByIndex = mapOf(0 to jsonText),
            valuesByName = mapOf("payload" to jsonText),
        )

        assertEquals(Payload("blue", 20), readable.getJackson<Payload>(0))
        assertEquals(Payload("blue", 20), readable.getJackson<Payload>("payload"))
    }

    @Test
    fun `Readable jackson3 json node getter supports text`() {
        val jsonText = """{"user":{"name":"tester"}}"""
        val readable = FakeReadable(valuesByIndex = mapOf(1 to jsonText))

        assertEquals("\"tester\"", readable.getJsonNode(1).path("user").path("name").toString())
    }

    @Test
    fun `Readable jackson3 nullable getter returns null when value is null`() {
        val readable = FakeReadable(valuesByIndex = mapOf(3 to null))
        assertNull(readable.getJacksonOrNull<Payload>(3))
        assertNull(readable.getJsonNodeOrNull(3))
    }

    @Test
    fun `Readable jackson3 non null getter throws descriptive error when value is null`() {
        val readable = FakeReadable(valuesByName = mapOf("payload" to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJsonNode("payload")
        }
        assertEquals("Column[payload] is null or not convertible to JsonNode.", ex.message)
    }
}
