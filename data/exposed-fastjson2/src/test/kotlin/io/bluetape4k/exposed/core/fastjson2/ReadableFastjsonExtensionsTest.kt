package io.bluetape4k.exposed.core.fastjson2

import io.r2dbc.spi.Readable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReadableFastjsonExtensionsTest {

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
    fun `Readable fastjson getter supports index and name`() {
        val jsonText = """{"name":"blue","age":20}"""
        val readable = FakeReadable(
            valuesByIndex = mapOf(0 to jsonText),
            valuesByName = mapOf("payload" to jsonText),
        )

        assertEquals(Payload("blue", 20), readable.getFastjson<Payload>(0))
        assertEquals(Payload("blue", 20), readable.getFastjson<Payload>("payload"))
    }

    @Test
    fun `Readable fastjson object and array getter supports text`() {
        val objectText = """{"user":{"name":"tester"}}"""
        val arrayText = """[1,2,3]"""
        val readable = FakeReadable(
            valuesByIndex = mapOf(1 to objectText, 2 to arrayText),
        )

        assertEquals("tester", readable.getFastjsonObject(1).getJSONObject("user").getString("name"))
        assertEquals(3, readable.getFastjsonArray(2).size)
    }

    @Test
    fun `Readable fastjson nullable getter returns null when value is null`() {
        val readable = FakeReadable(valuesByIndex = mapOf(3 to null))
        assertNull(readable.getFastjsonOrNull<Payload>(3))
        assertNull(readable.getFastjsonObjectOrNull(3))
        assertNull(readable.getFastjsonArrayOrNull(3))
    }

    @Test
    fun `Readable fastjson non null getter throws descriptive error when value is null`() {
        val readable = FakeReadable(valuesByName = mapOf("payload" to null))

        val ex = assertFailsWith<IllegalStateException> {
            readable.getFastjson<Payload>("payload")
        }
        assertEquals("Column[payload] is null or not convertible to Payload.", ex.message)
    }
}
