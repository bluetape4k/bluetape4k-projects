package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AssertSupportTest {

    companion object: KLogging()

    class TestClass

    @Test
    fun `assert without -ea`() {
        AssertSupportTest::class.java.classLoader.setClassAssertionStatus(
            AssertSupportTest::class.qualifiedName,
            false
        )
        AssertSupportTest::class.java.desiredAssertionStatus().shouldBeFalse()
    }

    @Test
    fun `assert with -ea`() {
        TestClass::class.java.classLoader.setClassAssertionStatus(TestClass::class.qualifiedName, true)
        TestClass::class.java.desiredAssertionStatus().shouldBeTrue()
    }

    // region assertNotNull / assertNull

    @Test
    fun `assert not null`() {
        var x: Long? = null
        assertFailsWith<AssertionError> {
            x.assertNotNull("x").toByteArray()
        }

        x = 12L
        x.assertNotNull("x")
    }

    @Test
    fun `assert null`() {
        val x: String? = null
        x.assertNull("x")

        assertFailsWith<AssertionError> {
            "hello".assertNull("x")
        }
    }

    // endregion

    // region CharSequence assertions

    @Test
    fun `assert not empty for string`() {
        var x: String? = null
        assertFailsWith<AssertionError> {
            x.assertNotEmpty("x")
        }

        x = ""
        assertFailsWith<AssertionError> {
            x.assertNotEmpty("x")
        }

        x = "    "
        x.assertNotEmpty("x")

        x = "  \t "
        x.assertNotEmpty("x")
    }

    @Test
    fun `assert null or empty for string`() {
        val empty: String? = null
        empty.assertNullOrEmpty("x")

        "".assertNullOrEmpty("x")

        assertFailsWith<AssertionError> {
            "hello".assertNullOrEmpty("x")
        }
    }

    @Test
    fun `assert not blank for string`() {
        var x: String? = null
        assertFailsWith<AssertionError> {
            x.assertNotBlank("x")
        }

        x = ""
        assertFailsWith<AssertionError> {
            x.assertNotBlank("x")
        }

        x = "    "
        assertFailsWith<AssertionError> {
            x.assertNotBlank("x")
        }

        x = "  \t "
        assertFailsWith<AssertionError> {
            x.assertNotBlank("x")
        }
    }

    @Test
    fun `assert null or blank for string`() {
        val empty: String? = null
        empty.assertNullOrBlank("x")

        "".assertNullOrBlank("x")
        "   ".assertNullOrBlank("x")

        assertFailsWith<AssertionError> {
            "hello".assertNullOrBlank("x")
        }
    }

    @Test
    fun `assert contains for string`() {
        "hello world".assertContains("world", "x")

        assertFailsWith<AssertionError> {
            "hello".assertContains("world", "x")
        }
    }

    @Test
    fun `assert starts with`() {
        "hello world".assertStartsWith("hello", "x")

        assertFailsWith<AssertionError> {
            "hello world".assertStartsWith("world", "x")
        }
    }

    @Test
    fun `assert starts with ignore case`() {
        "Hello World".assertStartsWith("hello", "x", ignoreCase = true)
    }

    @Test
    fun `assert ends with`() {
        "hello world".assertEndsWith("world", "x")

        assertFailsWith<AssertionError> {
            "hello world".assertEndsWith("hello", "x")
        }
    }

    @Test
    fun `assert ends with ignore case`() {
        "Hello World".assertEndsWith("WORLD", "x", ignoreCase = true)
    }

    // endregion

    // region Comparable assertions

    @Test
    fun `assert equals`() {
        42.assertEquals(42, "x")

        assertFailsWith<AssertionError> {
            42.assertEquals(99, "x")
        }
    }

    @Test
    fun `assert greater than`() {
        10.assertGt(5, "x")

        assertFailsWith<AssertionError> {
            5.assertGt(10, "x")
        }

        assertFailsWith<AssertionError> {
            5.assertGt(5, "x")
        }
    }

    @Test
    fun `assert greater than or equal`() {
        10.assertGe(5, "x")
        5.assertGe(5, "x")

        assertFailsWith<AssertionError> {
            4.assertGe(5, "x")
        }
    }

    @Test
    fun `assert less than`() {
        5.assertLt(10, "x")

        assertFailsWith<AssertionError> {
            10.assertLt(5, "x")
        }

        assertFailsWith<AssertionError> {
            5.assertLt(5, "x")
        }
    }

    @Test
    fun `assert less than or equal`() {
        5.assertLe(10, "x")
        5.assertLe(5, "x")

        assertFailsWith<AssertionError> {
            6.assertLe(5, "x")
        }
    }

    @Test
    fun `assert in range`() {
        5.assertInRange(1, 10, "x")
        1.assertInRange(1, 10, "x")
        10.assertInRange(1, 10, "x")

        assertFailsWith<AssertionError> {
            0.assertInRange(1, 10, "x")
        }

        assertFailsWith<AssertionError> {
            11.assertInRange(1, 10, "x")
        }
    }

    @Test
    fun `assert in open range`() {
        5.assertInOpenRange(1, 10, "x")
        1.assertInOpenRange(1, 10, "x")

        assertFailsWith<AssertionError> {
            10.assertInOpenRange(1, 10, "x")
        }
    }

    // endregion

    // region Number assertions

    @Test
    fun `assert positive number`() {
        1.assertPositiveNumber("x")
        0.1.assertPositiveNumber("x")

        assertFailsWith<AssertionError> {
            0.assertPositiveNumber("x")
        }

        assertFailsWith<AssertionError> {
            (-1).assertPositiveNumber("x")
        }
    }

    @Test
    fun `assert zero or positive number`() {
        0.assertZeroOrPositiveNumber("x")
        1.assertZeroOrPositiveNumber("x")

        assertFailsWith<AssertionError> {
            (-1).assertZeroOrPositiveNumber("x")
        }
    }

    @Test
    fun `assert negative number`() {
        (-1).assertNegativeNumber("x")

        assertFailsWith<AssertionError> {
            0.assertNegativeNumber("x")
        }

        assertFailsWith<AssertionError> {
            1.assertNegativeNumber("x")
        }
    }

    @Test
    fun `assert zero or negative number`() {
        0.assertZeroOrNegativeNumber("x")
        (-1).assertZeroOrNegativeNumber("x")

        assertFailsWith<AssertionError> {
            1.assertZeroOrNegativeNumber("x")
        }
    }

    // endregion

    // region Collection / Map assertions

    @Test
    fun `assert collection not empty`() {
        listOf(1, 2, 3).assertNotEmpty("x")

        assertFailsWith<AssertionError> {
            emptyList<Int>().assertNotEmpty("x")
        }

        assertFailsWith<AssertionError> {
            val nullList: List<Int>? = null
            nullList.assertNotEmpty("x")
        }
    }

    @Test
    fun `assert map not empty`() {
        mapOf("a" to 1).assertNotEmpty("x")

        assertFailsWith<AssertionError> {
            emptyMap<String, Int>().assertNotEmpty("x")
        }

        assertFailsWith<AssertionError> {
            val nullMap: Map<String, Int>? = null
            nullMap.assertNotEmpty("x")
        }
    }

    @Test
    fun `assert map has key`() {
        mapOf("a" to 1, "b" to 2).assertHasKey("a", "x")

        assertFailsWith<AssertionError> {
            mapOf("a" to 1).assertHasKey("b", "x")
        }
    }

    @Test
    fun `assert map has value`() {
        mapOf("a" to 1, "b" to 2).assertHasValue(1, "x")

        assertFailsWith<AssertionError> {
            mapOf("a" to 1).assertHasValue(99, "x")
        }
    }

    @Test
    fun `assert map contains key value pair`() {
        mapOf("a" to 1, "b" to 2).assertContains("a", 1, "x")

        assertFailsWith<AssertionError> {
            mapOf("a" to 1).assertContains("a", 99, "x")
        }
    }

    // endregion
}
