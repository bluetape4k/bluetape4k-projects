package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFailsWith

class AssertionsTest: AbstractExposedTest() {

    companion object: KLogging()

    @Test
    fun `assertTrue should pass when condition is true`() {
        withDb(TestDB.H2) {
            assertTrue(true)
        }
    }

    @Test
    fun `assertTrue should fail when condition is false`() {
        withDb(TestDB.H2) {
            val exception =
                assertFailsWith<AssertionError> {
                    assertTrue(false)
                }
            exception.message.shouldNotBeNull()
        }
    }

    @Test
    fun `assertFalse should pass when condition is false`() {
        withDb(TestDB.H2) {
            assertFalse(false)
        }
    }

    @Test
    fun `assertFalse should fail when condition is true`() {
        withDb(TestDB.H2) {
            val exception =
                assertFailsWith<AssertionError> {
                    assertFalse(true)
                }
            exception.message.shouldNotBeNull()
        }
    }

    @Test
    fun `assertEquals should pass when values are equal`() {
        withDb(TestDB.H2) {
            assertEquals(42, 42)
            assertEquals("test", "test")
        }
    }

    @Test
    fun `assertEquals should fail when values are not equal`() {
        withDb(TestDB.H2) {
            val exception =
                assertThrows<AssertionError> {
                    assertEquals(1, 2)
                }
            exception.message.shouldNotBeNull()
        }
    }

    @Test
    fun `assertEquals with collection should pass when collection contains single expected value`() {
        withDb(TestDB.H2) {
            assertEquals(42, listOf(42))
        }
    }

    @Test
    fun `assertEquals with collection should fail when collection is empty`() {
        withDb(TestDB.H2) {
            assertFailsWith<NoSuchElementException> {
                assertEquals(42, emptyList())
            }
        }
    }

    @Test
    fun `assertFailAndRollback should rollback on failure`() {
        withDb(TestDB.H2) {
            assertFailAndRollback("Test failure") {
                throw RuntimeException("Expected failure")
            }
        }
    }

    @Test
    fun `expectException should catch expected exception type`() {
        withDb(TestDB.H2) {
            expectException<IllegalArgumentException> {
                throw IllegalArgumentException("Test exception")
            }
        }
    }

    @Test
    fun `expectException should fail when exception type doesn't match`() {
        withDb(TestDB.H2) {
            assertFailsWith<AssertionError> {
                expectException<IllegalArgumentException> {
                    throw IllegalStateException("Wrong exception type")
                }
            }
        }
    }
}
