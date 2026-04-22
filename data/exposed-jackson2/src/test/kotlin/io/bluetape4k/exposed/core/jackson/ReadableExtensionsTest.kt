package io.bluetape4k.exposed.core.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.spi.Readable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReadableExtensionsTest {

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
    fun `Readable jackson getter supports index and name`() {
        val jsonText = """{"name":"blue","age":20}"""
        val readable = FakeReadable(
            valuesByIndex = mapOf(0 to jsonText),
            valuesByName = mapOf("payload" to jsonText),
        )

        assertEquals(Payload("blue", 20), readable.getJackson<Payload>(0))
        assertEquals(Payload("blue", 20), readable.getJackson<Payload>("payload"))
    }

    @Test
    fun `Readable jackson getter supports ByteArray value by index`() {
        val jsonText = """{"name":"bytes","age":5}"""
        val byteArrayValue = jsonText.toByteArray()
        val readable = FakeReadable(valuesByIndex = mapOf(0 to byteArrayValue))

        assertEquals(Payload("bytes", 5), readable.getJackson<Payload>(0))
    }

    @Test
    fun `Readable jackson getter supports ByteArray value by name`() {
        val jsonText = """{"name":"bytes","age":5}"""
        val byteArrayValue = jsonText.toByteArray()
        val readable = FakeReadable(valuesByName = mapOf("payload" to byteArrayValue))

        assertEquals(Payload("bytes", 5), readable.getJackson<Payload>("payload"))
    }

    @Test
    fun `Readable jackson getter returns T directly when value is already T by index`() {
        val payload = Payload("direct", 99)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to payload))

        assertEquals(payload, readable.getJackson<Payload>(0))
    }

    @Test
    fun `Readable jackson getter returns T directly when value is already T by name`() {
        val payload = Payload("direct", 99)
        val readable = FakeReadable(valuesByName = mapOf("payload" to payload))

        assertEquals(payload, readable.getJackson<Payload>("payload"))
    }

    @Test
    fun `Readable jackson getter handles else branch via toString by index`() {
        // Use an object whose toString() returns valid JSON
        val jsonText = """{"name":"tostring","age":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByIndex = mapOf(0 to customObj))

        assertEquals(Payload("tostring", 7), readable.getJackson<Payload>(0))
    }

    @Test
    fun `Readable jackson getter handles else branch via toString by name`() {
        val jsonText = """{"name":"tostring","age":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByName = mapOf("payload" to customObj))

        assertEquals(Payload("tostring", 7), readable.getJackson<Payload>("payload"))
    }

    @Test
    fun `Readable jackson json node getter supports text`() {
        val jsonText = """{"user":{"name":"tester"}}"""
        val readable = FakeReadable(valuesByIndex = mapOf(1 to jsonText))

        assertEquals("tester", readable.getJsonNode(1).path("user").path("name").textValue())
    }

    @Test
    fun `Readable jackson json node getter supports ByteArray by index`() {
        val jsonText = """{"user":{"name":"bytes"}}"""
        val readable = FakeReadable(valuesByIndex = mapOf(0 to jsonText.toByteArray()))

        assertEquals("bytes", readable.getJsonNode(0).path("user").path("name").textValue())
    }

    @Test
    fun `Readable jackson json node getter supports ByteArray by name`() {
        val jsonText = """{"user":{"name":"bytes"}}"""
        val readable = FakeReadable(valuesByName = mapOf("node" to jsonText.toByteArray()))

        assertEquals("bytes", readable.getJsonNode("node").path("user").path("name").textValue())
    }

    @Test
    fun `Readable jackson json node getter returns JsonNode directly when value is JsonNode by index`() {
        val mapper = ObjectMapper()
        val jsonNode: JsonNode = mapper.readTree("""{"x":1}""")
        val readable = FakeReadable(valuesByIndex = mapOf(0 to jsonNode))

        assertEquals(1, readable.getJsonNode(0).path("x").intValue())
    }

    @Test
    fun `Readable jackson json node getter returns JsonNode directly when value is JsonNode by name`() {
        val mapper = ObjectMapper()
        val jsonNode: JsonNode = mapper.readTree("""{"x":2}""")
        val readable = FakeReadable(valuesByName = mapOf("node" to jsonNode))

        assertEquals(2, readable.getJsonNode("node").path("x").intValue())
    }

    @Test
    fun `Readable jackson json node getter handles else branch via toString by index`() {
        val jsonText = """{"y":42}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByIndex = mapOf(0 to customObj))

        assertEquals(42, readable.getJsonNode(0).path("y").intValue())
    }

    @Test
    fun `Readable jackson json node getter handles else branch via toString by name`() {
        val jsonText = """{"y":42}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByName = mapOf("node" to customObj))

        assertEquals(42, readable.getJsonNode("node").path("y").intValue())
    }

    @Test
    fun `Readable jackson nullable getter returns null when value is null by index`() {
        val readable = FakeReadable(valuesByIndex = mapOf(3 to null))
        assertNull(readable.getJacksonOrNull<Payload>(3))
        assertNull(readable.getJsonNodeOrNull(3))
    }

    @Test
    fun `Readable jackson nullable getter returns null when key absent by name`() {
        val readable = FakeReadable()
        assertNull(readable.getJacksonOrNull<Payload>("missing"))
        assertNull(readable.getJsonNodeOrNull("missing"))
    }

    @Test
    fun `Readable jackson nullable getter returns result when value exists by name`() {
        val jsonText = """{"name":"present","age":1}"""
        val readable = FakeReadable(valuesByName = mapOf("payload" to jsonText))

        assertNotNull(readable.getJacksonOrNull<Payload>("payload"))
        assertNotNull(readable.getJsonNodeOrNull("payload"))
    }

    @Test
    fun `Readable jackson non null getter throws descriptive error when value is null`() {
        val readable = FakeReadable(valuesByName = mapOf("payload" to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJsonNode("payload")
        }
        assertEquals("Column[payload] is null or not convertible to JsonNode.", ex.message)
    }

    @Test
    fun `Readable jackson non null getter throws descriptive error when index value is null`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJackson<Payload>(0)
        }
        assertEquals("Column[0] is null or not convertible to Payload.", ex.message)
    }

    @Test
    fun `Readable jackson non null getter throws descriptive error for missing name`() {
        val readable = FakeReadable()

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJackson<Payload>("absent")
        }
        assertEquals("Column[absent] is null or not convertible to Payload.", ex.message)
    }
}
