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
        booleanArrayAttributeKeyOf("boolean.array.key") shouldBeEqualTo
            AttributeKey.booleanArrayKey("boolean.array.key")
        longArrayAttributeKeyOf("long.array.key") shouldBeEqualTo AttributeKey.longArrayKey("long.array.key")
        doubleArrayAttributeKeyOf("double.array.key") shouldBeEqualTo AttributeKey.doubleArrayKey("double.array.key")
    }

    @Test
    fun `create attribute keys with extension functions`() {
        "str".toAttributeKey() shouldBeEqualTo AttributeKey.stringKey("str")
        "str".toStringAttributeKey() shouldBeEqualTo AttributeKey.stringKey("str")
        "str".toStringArrayAttributeKey() shouldBeEqualTo AttributeKey.stringArrayKey("str")

        "bool".toBooleanAttributeKey() shouldBeEqualTo AttributeKey.booleanKey("bool")
        "bool".toBooleanArrayAttributeKey() shouldBeEqualTo AttributeKey.booleanArrayKey("bool")

        "lng".toLongAttributeKey() shouldBeEqualTo AttributeKey.longKey("lng")
        "lng".toLongArrayAttributeKey() shouldBeEqualTo AttributeKey.longArrayKey("lng")

        "dbl".toDoubleAttributeKey() shouldBeEqualTo AttributeKey.doubleKey("dbl")
        "dbl".toDoubleArrayAttributeKey() shouldBeEqualTo AttributeKey.doubleArrayKey("dbl")
    }

    @Test
    fun `primitive toAttributeKey extensions produce correct types`() {
        true.toAttributeKey() shouldBeEqualTo AttributeKey.booleanKey("true")
        42L.toAttributeKey() shouldBeEqualTo AttributeKey.longKey("42")
        3.14.toAttributeKey() shouldBeEqualTo AttributeKey.doubleKey("3.14")

        arrayOf("a", "b").toAttributeKey() shouldBeEqualTo AttributeKey.stringArrayKey("a,b")
        booleanArrayOf(true, false).toAttributeKey() shouldBeEqualTo AttributeKey.booleanArrayKey("true,false")
        longArrayOf(1L, 2L).toAttributeKey() shouldBeEqualTo AttributeKey.longArrayKey("1,2")
        doubleArrayOf(1.0, 2.5).toAttributeKey() shouldBeEqualTo AttributeKey.doubleArrayKey("1.0,2.5")
    }

    @Test
    fun `convert map to attributes`() {
        val map =
            mapOf(
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
