package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class AttributesSupportTest {

    @Test
    fun `map to attributes handles various value types`() {
        val floatArray = floatArrayOf(11.1f, 12.2f)

        val values = mapOf<Any?, Any?>(
            "string" to "v",
            "int" to 1,
            "long" to 2L,
            "float" to 3.5f,
            "double" to 4.5,
            "boolean" to true,
            "null" to null,
            "byteArray" to byteArrayOf(1, 2),
            "shortArray" to shortArrayOf(3, 4),
            "longArray" to longArrayOf(5L, 6L),
            "intArray" to intArrayOf(7, 8),
            "doubleArray" to doubleArrayOf(9.1, 10.2),
            "floatArray" to floatArray,
            "charArray" to charArrayOf('a', 'b'),
            "booleanArray" to booleanArrayOf(true, false),
            "stringArray" to arrayOf("x", null, "z"),
            "iterable" to listOf(3, 4),
            "sequence" to sequenceOf("seq", null),
        )

        val attributes = values.toAttributes()

        attributes[stringAttributeKeyOf("string")] shouldBeEqualTo "v"
        attributes[longAttributeKeyOf("int")] shouldBeEqualTo 1L
        attributes[longAttributeKeyOf("long")] shouldBeEqualTo 2L
        attributes[AttributeKey.doubleKey("float")] shouldBeEqualTo 3.5
        attributes[AttributeKey.doubleKey("double")] shouldBeEqualTo 4.5
        attributes[AttributeKey.booleanKey("boolean")].shouldNotBeNull().shouldBeTrue()
        attributes[stringAttributeKeyOf("null")] shouldBeEqualTo "null"

        attributes[AttributeKey.longArrayKey("byteArray")] shouldBeEqualTo listOf(1L, 2L)
        attributes[AttributeKey.longArrayKey("shortArray")] shouldBeEqualTo listOf(3L, 4L)
        attributes[AttributeKey.longArrayKey("intArray")] shouldBeEqualTo listOf(7L, 8L)
        attributes[AttributeKey.longArrayKey("longArray")] shouldBeEqualTo listOf(5L, 6L)
        attributes[AttributeKey.doubleArrayKey("doubleArray")] shouldBeEqualTo listOf(9.1, 10.2)
        attributes[AttributeKey.doubleArrayKey("floatArray")] shouldBeEqualTo floatArray.map { it.toDouble() }
        attributes[AttributeKey.stringArrayKey("charArray")] shouldBeEqualTo listOf("a", "b")
        attributes[AttributeKey.booleanArrayKey("booleanArray")] shouldBeEqualTo listOf(true, false)
        attributes[AttributeKey.stringArrayKey("stringArray")] shouldBeEqualTo listOf("x", "null", "z")
        attributes[AttributeKey.stringArrayKey("iterable")] shouldBeEqualTo listOf("3", "4")
        attributes[AttributeKey.stringArrayKey("sequence")] shouldBeEqualTo listOf("seq", "null")
    }
}
