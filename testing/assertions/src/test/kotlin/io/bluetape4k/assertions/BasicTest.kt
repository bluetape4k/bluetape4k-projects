package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class BasicTest {

    // ── shouldBeEqualTo / shouldNotBeEqualTo ──────────────────────────────

    @Test
    fun `shouldBeEqualTo passes when values are structurally equal`() {
        val a = "hello"
        val b = String(charArrayOf('h', 'e', 'l', 'l', 'o'))
        a shouldBeEqualTo b
    }

    @Test
    fun `shouldBeEqualTo fails when values are not equal`() {
        assertFailsWith<AssertionFailedError> {
            "hello" shouldBeEqualTo "world"
        }
    }

    @Test
    fun `shouldBeEqualTo passes for equal integers`() {
        42 shouldBeEqualTo 42
    }

    @Test
    fun `shouldBeEqualTo fails for unequal integers`() {
        assertFailsWith<AssertionFailedError> {
            1 shouldBeEqualTo 2
        }
    }

    @Test
    fun `shouldNotBeEqualTo passes when values differ`() {
        "hello" shouldNotBeEqualTo "world"
    }

    @Test
    fun `shouldNotBeEqualTo fails when values are equal`() {
        assertFailsWith<AssertionFailedError> {
            "hello" shouldNotBeEqualTo "hello"
        }
    }

    // ── shouldBe / shouldNotBe (referential ===) ─────────────────────────

    @Test
    fun `shouldBe passes when same reference`() {
        val obj = object {}
        obj shouldBe obj
    }

    @Test
    fun `shouldBe fails for different objects with same value`() {
        // Two separate String objects constructed differently are NOT the same reference
        val a = String(charArrayOf('h', 'e', 'l', 'l', 'o'))
        val b = String(charArrayOf('h', 'e', 'l', 'l', 'o'))
        assertFailsWith<AssertionFailedError> {
            a shouldBe b
        }
    }

    @Test
    fun `shouldBe vs shouldBeEqualTo distinction`() {
        val a = "hello"
        val b = String(charArrayOf('h', 'e', 'l', 'l', 'o'))

        // shouldBeEqualTo uses == (structural) — passes
        a shouldBeEqualTo b

        // shouldBe uses === (referential) — different objects, should fail
        assertFailsWith<AssertionFailedError> {
            a shouldBe b
        }
    }

    @Test
    fun `shouldNotBe passes for different references`() {
        val a = object {}
        val b = object {}
        a shouldNotBe b
    }

    @Test
    fun `shouldNotBe fails for same reference`() {
        val obj = "singleton"
        assertFailsWith<AssertionFailedError> {
            obj shouldNotBe obj
        }
    }

    // ── shouldBeNull / shouldNotBeNull ────────────────────────────────────

    @Test
    fun `shouldBeNull passes for null value`() {
        val value: String? = null
        value.shouldBeNull()
    }

    @Test
    fun `shouldBeNull fails for non-null value`() {
        assertFailsWith<AssertionFailedError> {
            "hello".shouldBeNull()
        }
    }

    @Test
    fun `shouldNotBeNull passes for non-null value`() {
        val value: String? = "hello"
        val nonNull = value.shouldNotBeNull()
        // smart-cast: nonNull is String (not String?)
        nonNull.length shouldBeEqualTo 5
    }

    @Test
    fun `shouldNotBeNull fails for null value`() {
        assertFailsWith<AssertionFailedError> {
            val value: String? = null
            value.shouldNotBeNull()
        }
    }

    @Test
    fun `shouldNotBeNull smart-cast compile verification`() {
        val s: String? = "hello"
        val nonNull = s.shouldNotBeNull()
        // This compiles only if smart-cast works: nonNull is String (not String?)
        nonNull.length shouldBeEqualTo 5
        nonNull.uppercase() shouldBeEqualTo "HELLO"
    }

    // ── shouldBeTrue / shouldBeFalse ──────────────────────────────────────

    @Test
    fun `shouldBeTrue passes for true`() {
        true.shouldBeTrue()
    }

    @Test
    fun `shouldBeTrue fails for false`() {
        assertFailsWith<AssertionFailedError> {
            false.shouldBeTrue()
        }
    }

    @Test
    fun `shouldBeTrue fails for null`() {
        assertFailsWith<AssertionFailedError> {
            val b: Boolean? = null
            b.shouldBeTrue()
        }
    }

    @Test
    fun `shouldBeFalse passes for false`() {
        false.shouldBeFalse()
    }

    @Test
    fun `shouldBeFalse fails for true`() {
        assertFailsWith<AssertionFailedError> {
            true.shouldBeFalse()
        }
    }

    @Test
    fun `shouldBeFalse fails for null`() {
        assertFailsWith<AssertionFailedError> {
            val b: Boolean? = null
            b.shouldBeFalse()
        }
    }

    // ── shouldNotBeTrue / shouldNotBeFalse ────────────────────────────────

    @Test
    fun `shouldNotBeTrue passes for false`() {
        false.shouldNotBeTrue()
    }

    @Test
    fun `shouldNotBeTrue passes for null`() {
        val b: Boolean? = null
        b.shouldNotBeTrue()
    }

    @Test
    fun `shouldNotBeTrue fails for true`() {
        assertFailsWith<AssertionFailedError> {
            true.shouldNotBeTrue()
        }
    }

    @Test
    fun `shouldNotBeFalse passes for true`() {
        true.shouldNotBeFalse()
    }

    @Test
    fun `shouldNotBeFalse passes for null`() {
        val b: Boolean? = null
        b.shouldNotBeFalse()
    }

    @Test
    fun `shouldNotBeFalse fails for false`() {
        assertFailsWith<AssertionFailedError> {
            false.shouldNotBeFalse()
        }
    }

    // ── should (custom predicate) ─────────────────────────────────────────

    @Test
    fun `should passes when predicate returns true`() {
        42.should("value should be positive") { it > 0 }
    }

    @Test
    fun `should fails when predicate returns false`() {
        assertFailsWith<AssertionFailedError> {
            (-1).should("value should be positive") { it > 0 }
        }
    }

    @Test
    fun `should supports chaining`() {
        "hello"
            .should("should not be empty") { it.isNotEmpty() }
            .should("should start with h") { it.startsWith("h") }
    }

    // ── fail ──────────────────────────────────────────────────────────────

    @Test
    fun `fail throws AssertionFailedError with message`() {
        val ex = assertFailsWith<AssertionFailedError> {
            fail("something went wrong")
        }
        ex.message shouldBeEqualTo "something went wrong"
    }

    @Test
    fun `fail with null message uses default message`() {
        val ex = assertFailsWith<AssertionFailedError> {
            fail()
        }
        ex.message shouldBeEqualTo "Test failed."
    }

    @Test
    fun `fail with cause wraps the throwable`() {
        val cause = RuntimeException("root cause")
        val ex = assertFailsWith<AssertionFailedError> {
            fail("wrapper message", cause)
        }
        ex.message shouldBeEqualTo "wrapper message"
        ex.cause shouldBe cause
    }

    // ── expectThat ────────────────────────────────────────────────────────

    @Test
    fun `expectThat passes when block returns expected value`() {
        expectThat(42) { 40 + 2 }
    }

    @Test
    fun `expectThat fails when block returns different value`() {
        assertFailsWith<AssertionFailedError> {
            expectThat(42) { 99 }
        }
    }

    @Test
    fun `expectThat with message passes when block returns expected value`() {
        expectThat("hello", "should be hello") { "hello" }
    }

    @Test
    fun `expectThat with message fails with provided message when values differ`() {
        val ex = assertFailsWith<AssertionFailedError> {
            expectThat("hello", "custom failure message") { "world" }
        }
        ex.message shouldBeEqualTo "custom failure message"
    }

    // ── shouldBeSameInstanceAs / shouldNotBeSameInstanceAs ────────────────

    @Test
    fun `shouldBeSameInstanceAs passes for same reference`() {
        val obj = "same"
        obj shouldBeSameInstanceAs obj
    }

    @Test
    fun `shouldBeSameInstanceAs fails for different references`() {
        val a = String(charArrayOf('x'))
        val b = String(charArrayOf('x'))
        assertFailsWith<AssertionFailedError> {
            a shouldBeSameInstanceAs b
        }
    }

    @Test
    fun `shouldNotBeSameInstanceAs passes for different references`() {
        val a = object {}
        val b = object {}
        a shouldNotBeSameInstanceAs b
    }

    @Test
    fun `shouldNotBeSameInstanceAs fails for same reference`() {
        val obj = "shared"
        assertFailsWith<AssertionFailedError> {
            obj shouldNotBeSameInstanceAs obj
        }
    }

    // ── chaining ─────────────────────────────────────────────────────────

    @Test
    fun `assertion functions support method chaining`() {
        "hello"
            .shouldNotBeNull()
            .shouldBeEqualTo("hello")
            .should("length is 5") { it.length == 5 }
    }
}
