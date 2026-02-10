package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
        assertFailsWith<IllegalArgumentException> {
            x.requireNotNull("x")
        }

        x = 12L
        x.requireNotNull("x")
    }

    @Test
    fun `require not empty for string`() {
        var x: String? = null
        assertFailsWith<IllegalArgumentException> {
            x.requireNotEmpty("x")
        }

        x = ""
        assertFailsWith<IllegalArgumentException> {
            x.requireNotEmpty("x")
        }

        x = "    "
        x.requireNotEmpty("x")

        x = "  \t "
        x.requireNotEmpty("x")
    }

    @Test
    fun `require not blank for string`() {
        var x: String? = null
        assertFailsWith<IllegalArgumentException> {
            x.requireNotBlank("x")
        }

        x = ""
        assertFailsWith<IllegalArgumentException> {
            x.requireNotBlank("x")
        }

        x = "    "
        assertFailsWith<IllegalArgumentException> {
            x.requireNotBlank("x")
        }

        x = "  \t "
        assertFailsWith<IllegalArgumentException> {
            x.requireNotBlank("x")
        }
    }

    // region requireNull / requireNullOrEmpty / requireNullOrBlank

    @Test
    fun `require null`() {
        val x: String? = null
        x.requireNull("x")

        assertFailsWith<IllegalArgumentException> {
            "hello".requireNull("x")
        }
    }

    @Test
    fun `require null or empty for string`() {
        val empty: String? = null
        empty.requireNullOrEmpty("x")

        "".requireNullOrEmpty("x")

        assertFailsWith<IllegalArgumentException> {
            "hello".requireNullOrEmpty("x")
        }
    }

    @Test
    fun `require null or blank for string`() {
        val empty: String? = null
        empty.requireNullOrBlank("x")

        "".requireNullOrBlank("x")
        "   ".requireNullOrBlank("x")

        assertFailsWith<IllegalArgumentException> {
            "hello".requireNullOrBlank("x")
        }
    }

    // endregion

    // region CharSequence assertions

    @Test
    fun `require contains for string`() {
        "hello world".requireContains("world", "x")

        assertFailsWith<IllegalArgumentException> {
            "hello".requireContains("world", "x")
        }
    }

    @Test
    fun `require starts with`() {
        "hello world".requireStartsWith("hello", "x")

        assertFailsWith<IllegalArgumentException> {
            "hello world".requireStartsWith("world", "x")
        }
    }

    @Test
    fun `require starts with ignore case`() {
        "Hello World".requireStartsWith("hello", "x", ignoreCase = true)
    }

    @Test
    fun `require ends with`() {
        "hello world".requireEndsWith("world", "x")

        assertFailsWith<IllegalArgumentException> {
            "hello world".requireEndsWith("hello", "x")
        }
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

        assertFailsWith<IllegalArgumentException> {
            42.requireEquals(99, "x")
        }
    }

    @Test
    fun `require greater than`() {
        10.requireGt(5, "x")

        assertFailsWith<IllegalArgumentException> {
            5.requireGt(10, "x")
        }

        assertFailsWith<IllegalArgumentException> {
            5.requireGt(5, "x")
        }
    }

    @Test
    fun `require greater than or equal`() {
        10.requireGe(5, "x")
        5.requireGe(5, "x")

        assertFailsWith<IllegalArgumentException> {
            4.requireGe(5, "x")
        }
    }

    @Test
    fun `require less than`() {
        5.requireLt(10, "x")

        assertFailsWith<IllegalArgumentException> {
            10.requireLt(5, "x")
        }

        assertFailsWith<IllegalArgumentException> {
            5.requireLt(5, "x")
        }
    }

    @Test
    fun `require less than or equal`() {
        5.requireLe(10, "x")
        5.requireLe(5, "x")

        assertFailsWith<IllegalArgumentException> {
            6.requireLe(5, "x")
        }
    }

    @Test
    fun `require in range`() {
        5.requireInRange(1, 10, "x")
        1.requireInRange(1, 10, "x")
        10.requireInRange(1, 10, "x")

        assertFailsWith<IllegalArgumentException> {
            0.requireInRange(1, 10, "x")
        }

        assertFailsWith<IllegalArgumentException> {
            11.requireInRange(1, 10, "x")
        }
    }

    @Test
    fun `require in open range`() {
        5.requireInOpenRange(1, 10, "x")
        1.requireInOpenRange(1, 10, "x")

        assertFailsWith<IllegalArgumentException> {
            10.requireInOpenRange(1, 10, "x")
        }
    }

    // endregion

    // region Number assertions

    @Test
    fun `require positive number`() {
        1.requirePositiveNumber("x")
        0.1.requirePositiveNumber("x")

        assertFailsWith<IllegalArgumentException> {
            0.requirePositiveNumber("x")
        }

        assertFailsWith<IllegalArgumentException> {
            (-1).requirePositiveNumber("x")
        }
    }

    @Test
    fun `require zero or positive number`() {
        0.requireZeroOrPositiveNumber("x")
        1.requireZeroOrPositiveNumber("x")

        assertFailsWith<IllegalArgumentException> {
            (-1).requireZeroOrPositiveNumber("x")
        }
    }

    @Test
    fun `require negative number`() {
        (-1).requireNegativeNumber("x")

        assertFailsWith<IllegalArgumentException> {
            0.requireNegativeNumber("x")
        }

        assertFailsWith<IllegalArgumentException> {
            1.requireNegativeNumber("x")
        }
    }

    @Test
    fun `require zero or negative number`() {
        0.requireZeroOrNegativeNumber("x")
        (-1).requireZeroOrNegativeNumber("x")

        assertFailsWith<IllegalArgumentException> {
            1.requireZeroOrNegativeNumber("x")
        }
    }

    // endregion

    // region Collection / Array / Map assertions

    @Test
    fun `require array not empty`() {
        arrayOf(1, 2, 3).requireNotEmpty("x")

        assertFailsWith<IllegalArgumentException> {
            emptyArray<Int>().requireNotEmpty("x")
        }

        assertFailsWith<IllegalArgumentException> {
            val nullArray: Array<Int>? = null
            nullArray.requireNotEmpty("x")
        }
    }

    @Test
    fun `require collection not empty`() {
        listOf(1, 2, 3).requireNotEmpty("x")

        assertFailsWith<IllegalArgumentException> {
            emptyList<Int>().requireNotEmpty("x")
        }

        assertFailsWith<IllegalArgumentException> {
            val nullList: List<Int>? = null
            nullList.requireNotEmpty("x")
        }
    }

    @Test
    fun `require map not empty`() {
        mapOf("a" to 1).requireNotEmpty("x")

        assertFailsWith<IllegalArgumentException> {
            emptyMap<String, Int>().requireNotEmpty("x")
        }

        assertFailsWith<IllegalArgumentException> {
            val nullMap: Map<String, Int>? = null
            nullMap.requireNotEmpty("x")
        }
    }

    @Test
    fun `require map has key`() {
        mapOf("a" to 1, "b" to 2).requireHasKey("a", "x")

        assertFailsWith<IllegalArgumentException> {
            mapOf("a" to 1).requireHasKey("b", "x")
        }
    }

    @Test
    fun `require map has value`() {
        mapOf("a" to 1, "b" to 2).requireHasValue(1, "x")

        assertFailsWith<IllegalArgumentException> {
            mapOf("a" to 1).requireHasValue(99, "x")
        }
    }

    @Test
    fun `require map contains key value pair`() {
        mapOf("a" to 1, "b" to 2).requireContains("a", 1, "x")

        assertFailsWith<IllegalArgumentException> {
            mapOf("a" to 1).requireContains("a", 99, "x")
        }
    }

    // endregion
}
