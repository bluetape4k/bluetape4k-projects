package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
import io.r2dbc.spi.Readable
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import tools.jackson.databind.JsonNode
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    fun `Readable jackson3 getter supports index and name`() {
        val jsonText = """{"name":"blue","age":20}"""
        val readable = FakeReadable(
            valuesByIndex = mapOf(0 to jsonText),
            valuesByName = mapOf("payload" to jsonText),
        )

        readable.getJackson<Payload>(0) shouldBeEqualTo Payload("blue", 20)
        readable.getJackson<Payload>("payload") shouldBeEqualTo Payload("blue", 20)
    }

    @Test
    fun `Readable jackson3 getter supports ByteArray value by index`() {
        val jsonText = """{"name":"bytes","age":5}"""
        val byteArrayValue = jsonText.toByteArray()
        val readable = FakeReadable(valuesByIndex = mapOf(0 to byteArrayValue))

        readable.getJackson<Payload>(0) shouldBeEqualTo Payload("bytes", 5)
    }

    @Test
    fun `Readable jackson3 getter supports ByteArray value by name`() {
        val jsonText = """{"name":"bytes","age":5}"""
        val byteArrayValue = jsonText.toByteArray()
        val readable = FakeReadable(valuesByName = mapOf("payload" to byteArrayValue))

        readable.getJackson<Payload>("payload") shouldBeEqualTo Payload("bytes", 5)
    }

    @Test
    fun `Readable jackson3 getter returns T directly when value is already T by index`() {
        val payload = Payload("direct", 99)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to payload))

        readable.getJackson<Payload>(0) shouldBeEqualTo payload
    }

    @Test
    fun `Readable jackson3 getter returns T directly when value is already T by name`() {
        val payload = Payload("direct", 99)
        val readable = FakeReadable(valuesByName = mapOf("payload" to payload))

        readable.getJackson<Payload>("payload") shouldBeEqualTo payload
    }

    @Test
    fun `Readable jackson3 getter handles else branch via toString by index`() {
        val jsonText = """{"name":"tostring","age":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByIndex = mapOf(0 to customObj))

        readable.getJackson<Payload>(0) shouldBeEqualTo Payload("tostring", 7)
    }

    @Test
    fun `Readable jackson3 getter handles else branch via toString by name`() {
        val jsonText = """{"name":"tostring","age":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByName = mapOf("payload" to customObj))

        readable.getJackson<Payload>("payload") shouldBeEqualTo Payload("tostring", 7)
    }

    @Test
    fun `Readable jackson3 json node getter supports text`() {
        val jsonText = """{"user":{"name":"tester"}}"""
        val readable = FakeReadable(valuesByIndex = mapOf(1 to jsonText))

        readable.getJsonNode(1).path("user").path("name").toString() shouldBeEqualTo "\"tester\""
    }

    @Test
    fun `Readable jackson3 json node getter supports ByteArray by index`() {
        val jsonText = """{"user":{"name":"bytes"}}"""
        val readable = FakeReadable(valuesByIndex = mapOf(0 to jsonText.toByteArray()))

        readable.getJsonNode(0).path("user").path("name").asText() shouldBeEqualTo "bytes"
    }

    @Test
    fun `Readable jackson3 json node getter supports ByteArray by name`() {
        val jsonText = """{"user":{"name":"bytes"}}"""
        val readable = FakeReadable(valuesByName = mapOf("node" to jsonText.toByteArray()))

        readable.getJsonNode("node").path("user").path("name").asText() shouldBeEqualTo "bytes"
    }

    @Test
    fun `Readable jackson3 json node getter returns JsonNode directly when value is JsonNode by index`() {
        val jsonNode: JsonNode = DefaultJacksonSerializer.mapper.readTree("""{"x":1}""")
        val readable = FakeReadable(valuesByIndex = mapOf(0 to jsonNode))

        readable.getJsonNode(0) shouldBeEqualTo jsonNode
    }

    @Test
    fun `Readable jackson3 json node getter returns JsonNode directly when value is JsonNode by name`() {
        val jsonNode: JsonNode = DefaultJacksonSerializer.mapper.readTree("""{"x":2}""")
        val readable = FakeReadable(valuesByName = mapOf("node" to jsonNode))

        readable.getJsonNode("node") shouldBeEqualTo jsonNode
    }

    @Test
    fun `Readable jackson3 json node getter handles else branch via toString by index`() {
        val jsonText = """{"y":42}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByIndex = mapOf(0 to customObj))

        readable.getJsonNode(0).path("y").asInt() shouldBeEqualTo 42
    }

    @Test
    fun `Readable jackson3 json node getter handles else branch via toString by name`() {
        val jsonText = """{"y":42}"""
        val customObj = object {
            override fun toString() = jsonText
        }
        val readable = FakeReadable(valuesByName = mapOf("node" to customObj))

        readable.getJsonNode("node").path("y").asInt() shouldBeEqualTo 42
    }

    @Test
    fun `Readable jackson3 nullable getter returns null when value is null by index`() {
        val readable = FakeReadable(valuesByIndex = mapOf(3 to null))
        readable.getJacksonOrNull<Payload>(3).shouldBeNull()
        readable.getJsonNodeOrNull(3).shouldBeNull()
    }

    @Test
    fun `Readable jackson3 nullable getter returns null when key absent by name`() {
        val readable = FakeReadable()
        readable.getJacksonOrNull<Payload>("missing").shouldBeNull()
        readable.getJsonNodeOrNull("missing").shouldBeNull()
    }

    @Test
    fun `Readable jackson3 nullable getter returns result when value exists by name`() {
        val jsonText = """{"name":"present","age":1}"""
        val readable = FakeReadable(valuesByName = mapOf("payload" to jsonText))

        readable.getJacksonOrNull<Payload>("payload").shouldNotBeNull()
        readable.getJsonNodeOrNull("payload").shouldNotBeNull()
    }

    @Test
    fun `Readable jackson3 non null getter throws descriptive error when value is null`() {
        val readable = FakeReadable(valuesByName = mapOf("payload" to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJsonNode("payload")
        }
        ex.message shouldBeEqualTo "Column[payload] is null or not convertible to JsonNode."
    }

    @Test
    fun `Readable jackson3 non null getter throws descriptive error when index value is null`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJackson<Payload>(0)
        }
        ex.message shouldBeEqualTo "Column[0] is null or not convertible to Payload."
    }

    @Test
    fun `Readable jackson3 non null getter throws descriptive error for missing name`() {
        val readable = FakeReadable()

        val ex = assertFailsWith<IllegalStateException> {
            readable.getJackson<Payload>("absent")
        }
        ex.message shouldBeEqualTo "Column[absent] is null or not convertible to Payload."
    }
}
