package io.bluetape4k.micrometer.common

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class KeyValueSupportTest {

    @Test
    fun `keyValuesOf should enforce even number of arguments and valid keys`() {
        val keyValues = keyValuesOf("alpha", "1", "beta", "2")
        keyValues.toList().associate { it.key to it.value } shouldBeEqualTo mapOf(
            "alpha" to "1",
            "beta" to "2",
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
        result.toList().associate { it.key to it.value } shouldBeEqualTo mapOf(
            "first" to "1",
            "second" to "2",
        )
    }
}
