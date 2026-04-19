package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class RequireSupportTest {

    companion object: KLogging()

    @Test
    fun `assert without -ea`() {
        RequireSupportTest::class.java.classLoader.setClassAssertionStatus(
            RequireSupportTest::class.qualifiedName,
            false
        )
        RequireSupportTest::class.java.desiredAssertionStatus().shouldBeFalse()
    }

    class TestClass

    @Test
    fun `assert with -ea`() {
        TestClass::class.java.classLoader.setClassAssertionStatus(TestClass::class.qualifiedName, true)
        TestClass::class.java.desiredAssertionStatus().shouldBeTrue()
    }

    @Test
    fun `require not null`() {
        var x: Long? = null
        shouldFailRequire { x.requireNotNull("x") }

        x = 12L
        x.requireNotNull("x")
    }

    @Test
    fun `require not empty for string`() {
        var x: String? = null
        shouldFailRequire { x.requireNotEmpty("x") }

        x = ""
        shouldFailRequire { x.requireNotEmpty("x") }

        x = "    "
        x.requireNotEmpty("x")

        x = "  \t "
        x.requireNotEmpty("x")
    }

    @Test
    fun `require not blank for string`() {
        var x: String? = null
        shouldFailRequire { x.requireNotBlank("x") }

        x = ""
        shouldFailRequire { x.requireNotBlank("x") }

        x = "    "
        shouldFailRequire { x.requireNotBlank("x") }

        x = "  \t "
        shouldFailRequire { x.requireNotBlank("x") }
    }

    // region requireNull / requireNullOrEmpty / requireNullOrBlank

    @Test
    fun `require null`() {
        val x: String? = null
        x.requireNull("x")

        shouldFailRequire { "hello".requireNull("x") }
    }

    @Test
    fun `require null or empty for string`() {
        val empty: String? = null
        empty.requireNullOrEmpty("x")

        "".requireNullOrEmpty("x")

        shouldFailRequire { "hello".requireNullOrEmpty("x") }
    }

    @Test
    fun `require null or blank for string`() {
        val empty: String? = null
        empty.requireNullOrBlank("x")

        "".requireNullOrBlank("x")
        "   ".requireNullOrBlank("x")

        shouldFailRequire { "hello".requireNullOrBlank("x") }
    }

    // endregion

    // region CharSequence assertions

    @Test
    fun `require contains for string`() {
        "hello world".requireContains("world", "x")

        shouldFailRequire { "hello".requireContains("world", "x") }
    }

    @Test
    fun `require starts with`() {
        "hello world".requireStartsWith("hello", "x")

        shouldFailRequire { "hello world".requireStartsWith("world", "x") }
    }

    @Test
    fun `require starts with ignore case`() {
        "Hello World".requireStartsWith("hello", "x", ignoreCase = true)
    }

    @Test
    fun `require ends with`() {
        "hello world".requireEndsWith("world", "x")

        shouldFailRequire { "hello world".requireEndsWith("hello", "x") }
    }

    @Test
    fun `require ends with ignore case`() {
        "Hello World".requireEndsWith("WORLD", "x", ignoreCase = true)
    }

    // endregion

    // region Comparable assertions

    @Test
    fun `require equals`() {
        42.requireEquals(42, "x")

        shouldFailRequire { 42.requireEquals(99, "x") }
    }

    @Test
    fun `require greater than`() {
        10.requireGt(5, "x")

        shouldFailRequire { 5.requireGt(10, "x") }

        shouldFailRequire { 5.requireGt(5, "x") }
    }

    @Test
    fun `require greater than or equal`() {
        10.requireGe(5, "x")
        5.requireGe(5, "x")

        shouldFailRequire { 4.requireGe(5, "x") }
    }

    @Test
    fun `require less than`() {
        5.requireLt(10, "x")

        shouldFailRequire { 10.requireLt(5, "x") }

        shouldFailRequire { 5.requireLt(5, "x") }
    }

    @Test
    fun `require less than or equal`() {
        5.requireLe(10, "x")
        5.requireLe(5, "x")

        shouldFailRequire { 6.requireLe(5, "x") }
    }

    @Test
    fun `require in range`() {
        5.requireInRange(1, 10, "x")
        1.requireInRange(1, 10, "x")
        10.requireInRange(1, 10, "x")

        shouldFailRequire { 0.requireInRange(1, 10, "x") }

        shouldFailRequire { 11.requireInRange(1, 10, "x") }
    }

    @Test
    fun `require in open range`() {
        5.requireInOpenRange(1, 10, "x")
        1.requireInOpenRange(1, 10, "x")

        shouldFailRequire { 10.requireInOpenRange(1, 10, "x") }
    }

    // endregion

    // region Number assertions

    @Test
    fun `require positive number`() {
        1.requirePositiveNumber("x")
        0.1.requirePositiveNumber("x")

        shouldFailRequire { 0.requirePositiveNumber("x") }

        shouldFailRequire { (-1).requirePositiveNumber("x") }
    }

    @Test
    fun `require zero or positive number`() {
        0.requireZeroOrPositiveNumber("x")
        1.requireZeroOrPositiveNumber("x")

        shouldFailRequire { (-1).requireZeroOrPositiveNumber("x") }
    }

    @Test
    fun `require negative number`() {
        (-1).requireNegativeNumber("x")

        shouldFailRequire { 0.requireNegativeNumber("x") }

        shouldFailRequire { 1.requireNegativeNumber("x") }
    }

    @Test
    fun `require zero or negative number`() {
        0.requireZeroOrNegativeNumber("x")
        (-1).requireZeroOrNegativeNumber("x")

        shouldFailRequire { 1.requireZeroOrNegativeNumber("x") }
    }

    // endregion

    // region Collection / Array / Map assertions

    @Test
    fun `require array not empty`() {
        arrayOf(1, 2, 3).requireNotEmpty("x")

        shouldFailRequire { emptyArray<Int>().requireNotEmpty("x") }

        shouldFailRequire {
            val nullArray: Array<Int>? = null
            nullArray.requireNotEmpty("x")
        }
    }

    @Test
    fun `require collection not empty`() {
        listOf(1, 2, 3).requireNotEmpty("x")

        shouldFailRequire { emptyList<Int>().requireNotEmpty("x") }

        shouldFailRequire {
            val nullList: List<Int>? = null
            nullList.requireNotEmpty("x")
        }
    }

    @Test
    fun `require map not empty`() {
        mapOf("a" to 1).requireNotEmpty("x")

        shouldFailRequire { emptyMap<String, Int>().requireNotEmpty("x") }

        shouldFailRequire {
            val nullMap: Map<String, Int>? = null
            nullMap.requireNotEmpty("x")
        }
    }

    @Test
    fun `require map has key`() {
        mapOf("a" to 1, "b" to 2).requireHasKey("a", "x")

        shouldFailRequire { mapOf("a" to 1).requireHasKey("b", "x") }
    }

    @Test
    fun `require map has value`() {
        mapOf("a" to 1, "b" to 2).requireHasValue(1, "x")

        shouldFailRequire { mapOf("a" to 1).requireHasValue(99, "x") }
    }

    @Test
    fun `require map contains key value pair`() {
        mapOf("a" to 1, "b" to 2).requireContains("a", 1, "x")

        shouldFailRequire { mapOf("a" to 1).requireContains("a", 99, "x") }
    }

    // endregion
}
