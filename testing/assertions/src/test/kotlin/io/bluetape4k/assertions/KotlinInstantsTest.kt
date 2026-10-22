package io.bluetape4k.assertions

import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import kotlin.time.Instant

class KotlinInstantsTest {

    private val now = Instant.parse("2026-05-07T10:00:00Z")
    private val before = Instant.parse("2026-05-07T09:00:00Z")
    private val after = Instant.parse("2026-05-07T11:00:00Z")

    @Test
    fun `shouldBeAfter passes when receiver is after expected`() {
        after shouldBeAfter now
    }

    @Test
    fun `shouldBeAfter fails when receiver is equal to expected`() {
        assertFailsWith<AssertionFailedError> {
            now shouldBeAfter now
        }
    }

    @Test
    fun `shouldBeAfter fails when receiver is before expected`() {
        assertFailsWith<AssertionFailedError> {
            before shouldBeAfter now
        }
    }

    @Test
    fun `shouldBeBefore passes when receiver is before expected`() {
        before shouldBeBefore now
    }

    @Test
    fun `shouldBeBefore fails when receiver is equal to expected`() {
        assertFailsWith<AssertionFailedError> {
            now shouldBeBefore now
        }
    }

    @Test
    fun `shouldBeBefore fails when receiver is after expected`() {
        assertFailsWith<AssertionFailedError> {
            after shouldBeBefore now
        }
    }

    @Test
    fun `shouldBeOnOrAfter passes when receiver is after expected`() {
        after shouldBeOnOrAfter now
    }

    @Test
    fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
        now shouldBeOnOrAfter now
    }

    @Test
    fun `shouldBeOnOrAfter fails when receiver is before expected`() {
        assertFailsWith<AssertionFailedError> {
            before shouldBeOnOrAfter now
        }
    }

    @Test
    fun `shouldBeOnOrBefore passes when receiver is before expected`() {
        before shouldBeOnOrBefore now
    }

    @Test
    fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
        now shouldBeOnOrBefore now
    }

    @Test
    fun `shouldBeOnOrBefore fails when receiver is after expected`() {
        assertFailsWith<AssertionFailedError> {
            after shouldBeOnOrBefore now
        }
    }

    @Test
    fun `shouldBeAfter supports chaining`() {
        after
            .shouldBeAfter(now)
            .shouldBeAfter(before)
    }
}
