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
}
