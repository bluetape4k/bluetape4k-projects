package io.bluetape4k.opentelemetry.common

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.opentelemetry.AbstractOtelTest
import org.junit.jupiter.api.Test

class AttributesSupportTest: AbstractOtelTest() {

    companion object: KLogging()

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

        log.debug { "attributes=$attributes" }

        attributes["string".toStringAttributeKey()] shouldBeEqualTo "v"
        attributes["int".toLongAttributeKey()] shouldBeEqualTo 1L
        attributes["long".toLongAttributeKey()] shouldBeEqualTo 2L
        attributes["float".toDoubleAttributeKey()] shouldBeEqualTo 3.5
        attributes["double".toDoubleAttributeKey()] shouldBeEqualTo 4.5
        attributes["boolean".toBooleanAttributeKey()].shouldNotBeNull().shouldBeTrue()
        attributes["null".toStringAttributeKey()] shouldBeEqualTo "null"

        attributes["byteArray".toLongArrayAttributeKey()] shouldBeEqualTo listOf(1L, 2L)
        attributes["shortArray".toLongArrayAttributeKey()] shouldBeEqualTo listOf(3L, 4L)
        attributes["intArray".toLongArrayAttributeKey()] shouldBeEqualTo listOf(7L, 8L)
        attributes["longArray".toLongArrayAttributeKey()] shouldBeEqualTo listOf(5L, 6L)
        attributes["doubleArray".toDoubleArrayAttributeKey()] shouldBeEqualTo listOf(9.1, 10.2)
        attributes["floatArray".toDoubleArrayAttributeKey()] shouldBeEqualTo floatArray.map { it.toDouble() }
        attributes["charArray".toStringArrayAttributeKey()] shouldBeEqualTo listOf("a", "b")
        attributes["booleanArray".toBooleanArrayAttributeKey()] shouldBeEqualTo listOf(true, false)
        attributes["stringArray".toStringArrayAttributeKey()] shouldBeEqualTo listOf("x", "null", "z")
        attributes["iterable".toStringArrayAttributeKey()] shouldBeEqualTo listOf("3", "4")
        attributes["sequence".toStringArrayAttributeKey()] shouldBeEqualTo listOf("seq", "null")
    }
}
