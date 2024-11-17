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
}
