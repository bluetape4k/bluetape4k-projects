package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class AttributeKeySupportTest {

    @Test
    fun `create attribute keys with of function`() {
        stringAttributeKeyOf("string.key") shouldBeEqualTo AttributeKey.stringKey("string.key")
        booleanAttributeKeyOf("boolean.key") shouldBeEqualTo AttributeKey.booleanKey("boolean.key")
        longAttributeKeyOf("long.key") shouldBeEqualTo AttributeKey.longKey("long.key")
        doubleAttributeKeyOf("double.key") shouldBeEqualTo AttributeKey.doubleKey("double.key")

        stringArrayAttributeKeyOf("string.array.key") shouldBeEqualTo AttributeKey.stringArrayKey("string.array.key")
        booleanArrayAttributeKeyOf("boolean.array.key") shouldBeEqualTo AttributeKey.booleanArrayKey("boolean.array.key")
        longArrayAttributeKeyOf("long.array.key") shouldBeEqualTo AttributeKey.longArrayKey("long.array.key")
        doubleArrayAttributeKeyOf("double.array.key") shouldBeEqualTo AttributeKey.doubleArrayKey("double.array.key")
    }

    @Test
    fun `convert map to attributes`() {
        val map = mapOf(
            "string.key" to "string.value",
            "int.key" to 123,
            "long.key" to 123L,
            "double.key" to 123.456,
            "boolean.key" to true,
            "string.array.key" to arrayOf("a", "b", "c"),
            "long.array.key" to longArrayOf(1, 2, 3)
        )

        val attributes = map.toAttributes()

        attributes[AttributeKey.stringKey("string.key")] shouldBeEqualTo "string.value"
        attributes[AttributeKey.longKey("int.key")] shouldBeEqualTo 123L
        attributes[AttributeKey.longKey("long.key")] shouldBeEqualTo 123L
        attributes[AttributeKey.doubleKey("double.key")] shouldBeEqualTo 123.456
        attributes[AttributeKey.booleanKey("boolean.key")].shouldNotBeNull().shouldBeTrue()
        attributes[AttributeKey.stringArrayKey("string.array.key")] shouldBeEqualTo listOf("a", "b", "c")
        attributes[AttributeKey.longArrayKey("long.array.key")] shouldBeEqualTo listOf(1L, 2L, 3L)
    }
}
