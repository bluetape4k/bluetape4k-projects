package io.bluetape4k.micrometer.common

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class KeyValueSupportTest {
    @Test
    fun `keyValuesOf should enforce even number of arguments and valid keys`() {
        val keyValues = keyValuesOf("alpha", "1", "beta", "2")
        keyValues.toList().associate { it.key to it.value } shouldBeEqualTo
            mapOf(
                "alpha" to "1",
                "beta" to "2"
            )

        assertFailsWith<IllegalArgumentException> {
            keyValuesOf("only-key")
        }
        assertFailsWith<IllegalArgumentException> {
            keyValuesOf("", "value")
        }
    }

    @Test
    fun `keyValueOf map should reject blank keys`() {
        keyValueOf(mapOf("foo" to "bar")).toList().first().key shouldBeEqualTo "foo"
        assertFailsWith<IllegalArgumentException> {
            keyValueOf(mapOf(" " to "blank"))
        }
    }

    @Test
    fun `keyValuesOf pair array defers to map helper`() {
        val pairs = arrayOf("first" to "1", "second" to "2")
        val result = keyValuesOf(*pairs)
        result.toList().associate { it.key to it.value } shouldBeEqualTo
            mapOf(
                "first" to "1",
                "second" to "2"
            )
    }

    @Test
    fun `keyValuesOf pair array should validate keys consistently`() {
        assertFailsWith<IllegalArgumentException> {
            keyValuesOf("" to "1")
        }
    }

    @Test
    fun `keyValueOf with validator should accept valid values`() {
        val kv = keyValueOf("count", 42) { it > 0 }
        kv.key shouldBeEqualTo "count"
        kv.value shouldBeEqualTo "42"
    }

    @Test
    fun `keyValueOf should reject blank key`() {
        assertFailsWith<IllegalArgumentException> {
            keyValueOf("  ", "value")
        }
    }

    @Test
    fun `keyValuesOf from KeyValue vararg should create KeyValues`() {
        val kv1 = keyValueOf("a", "1")
        val kv2 = keyValueOf("b", "2")
        val result = keyValuesOf(kv1, kv2)
        result.toList().associate { it.key to it.value } shouldBeEqualTo mapOf("a" to "1", "b" to "2")
    }

    @Test
    fun `keyValueOf from iterable should create KeyValues`() {
        val items = listOf(keyValueOf("x", "10"), keyValueOf("y", "20"))
        val result = keyValueOf(items)
        result.toList().associate { it.key to it.value } shouldBeEqualTo mapOf("x" to "10", "y" to "20")
    }
}
