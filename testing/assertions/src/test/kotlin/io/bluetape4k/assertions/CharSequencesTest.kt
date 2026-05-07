package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class CharSequencesTest {

    // ── shouldStartWith / shouldNotStartWith ──────────────────────────────

    @Test
    fun `shouldStartWith passes when CharSequence starts with prefix`() {
        "hello world" shouldStartWith "hello"
    }

    @Test
    fun `shouldStartWith fails when CharSequence does not start with prefix`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldStartWith "world"
        }
    }

    @Test
    fun `shouldStartWith fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldStartWith "hello"
        }
    }

    @Test
    fun `shouldStartWith supports chaining`() {
        "hello world"
            .shouldStartWith("hello")
            .shouldEndWith("world")
    }

    @Test
    fun `shouldNotStartWith passes when CharSequence does not start with prefix`() {
        "hello world" shouldNotStartWith "world"
    }

    @Test
    fun `shouldNotStartWith passes for null receiver`() {
        val s: CharSequence? = null
        s shouldNotStartWith "hello"
    }

    @Test
    fun `shouldNotStartWith fails when CharSequence starts with prefix`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldNotStartWith "hello"
        }
    }

    // ── shouldEndWith / shouldNotEndWith ──────────────────────────────────

    @Test
    fun `shouldEndWith passes when CharSequence ends with suffix`() {
        "hello world" shouldEndWith "world"
    }

    @Test
    fun `shouldEndWith fails when CharSequence does not end with suffix`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldEndWith "hello"
        }
    }

    @Test
    fun `shouldEndWith fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldEndWith "world"
        }
    }

    @Test
    fun `shouldNotEndWith passes when CharSequence does not end with suffix`() {
        "hello world" shouldNotEndWith "hello"
    }

    @Test
    fun `shouldNotEndWith passes for null receiver`() {
        val s: CharSequence? = null
        s shouldNotEndWith "world"
    }

    @Test
    fun `shouldNotEndWith fails when CharSequence ends with suffix`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldNotEndWith "world"
        }
    }

    // ── shouldContain / shouldNotContain ──────────────────────────────────

    @Test
    fun `shouldContain passes when CharSequence contains substring`() {
        "hello world" shouldContain "lo wo"
    }

    @Test
    fun `shouldContain fails when CharSequence does not contain substring`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldContain "xyz"
        }
    }

    @Test
    fun `shouldContain fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldContain "hello"
        }
    }

    @Test
    fun `shouldNotContain passes when CharSequence does not contain substring`() {
        "hello world" shouldNotContain "xyz"
    }

    @Test
    fun `shouldNotContain passes for null receiver`() {
        val s: CharSequence? = null
        s shouldNotContain "hello"
    }

    @Test
    fun `shouldNotContain fails when CharSequence contains substring`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldNotContain "lo wo"
        }
    }

    // ── shouldContainIgnoringCase ──────────────────────────────────────────

    @Test
    fun `shouldContainIgnoringCase passes with matching case`() {
        "Hello World" shouldContainIgnoringCase "hello"
    }

    @Test
    fun `shouldContainIgnoringCase passes with different case`() {
        "Hello World" shouldContainIgnoringCase "WORLD"
    }

    @Test
    fun `shouldContainIgnoringCase fails when substring not present`() {
        assertFailsWith<AssertionFailedError> {
            "Hello World" shouldContainIgnoringCase "xyz"
        }
    }

    @Test
    fun `shouldContainIgnoringCase fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldContainIgnoringCase "hello"
        }
    }

    // ── shouldBeEmpty / shouldNotBeEmpty ──────────────────────────────────

    @Test
    fun `shouldBeEmpty passes for empty string`() {
        "".shouldBeEmpty()
    }

    @Test
    fun `shouldBeEmpty passes for null receiver`() {
        val s: CharSequence? = null
        s.shouldBeEmpty()
    }

    @Test
    fun `shouldBeEmpty fails for non-empty string`() {
        assertFailsWith<AssertionFailedError> {
            "hello".shouldBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty passes for non-empty string`() {
        "hello".shouldNotBeEmpty()
    }

    @Test
    fun `shouldNotBeEmpty fails for empty string`() {
        assertFailsWith<AssertionFailedError> {
            "".shouldNotBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s.shouldNotBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty returns non-null CharSequence`() {
        val s: CharSequence? = "hello"
        // Return type is CharSequence (non-null), allowing non-null operations
        val result: CharSequence = s.shouldNotBeEmpty()
        result.length shouldBeEqualTo 5
    }

    // ── shouldBeBlank / shouldNotBeBlank ──────────────────────────────────

    @Test
    fun `shouldBeBlank passes for blank string`() {
        "   ".shouldBeBlank()
    }

    @Test
    fun `shouldBeBlank passes for empty string`() {
        "".shouldBeBlank()
    }

    @Test
    fun `shouldBeBlank passes for null receiver`() {
        val s: CharSequence? = null
        s.shouldBeBlank()
    }

    @Test
    fun `shouldBeBlank fails for non-blank string`() {
        assertFailsWith<AssertionFailedError> {
            "hello".shouldBeBlank()
        }
    }

    @Test
    fun `shouldNotBeBlank passes for non-blank string`() {
        "hello".shouldNotBeBlank()
    }

    @Test
    fun `shouldNotBeBlank fails for blank string`() {
        assertFailsWith<AssertionFailedError> {
            "   ".shouldNotBeBlank()
        }
    }

    @Test
    fun `shouldNotBeBlank fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s.shouldNotBeBlank()
        }
    }

    @Test
    fun `shouldNotBeBlank returns non-null CharSequence`() {
        val s: CharSequence? = "hello"
        val result: CharSequence = s.shouldNotBeBlank()
        result.length shouldBeEqualTo 5
    }

    // ── shouldBeNullOrEmpty / shouldNotBeNullOrEmpty ──────────────────────

    @Test
    fun `shouldBeNullOrEmpty passes for null`() {
        val s: CharSequence? = null
        s.shouldBeNullOrEmpty()
    }

    @Test
    fun `shouldBeNullOrEmpty passes for empty string`() {
        "".shouldBeNullOrEmpty()
    }

    @Test
    fun `shouldBeNullOrEmpty fails for non-empty string`() {
        assertFailsWith<AssertionFailedError> {
            "hello".shouldBeNullOrEmpty()
        }
    }

    @Test
    fun `shouldNotBeNullOrEmpty passes for non-empty string`() {
        "hello".shouldNotBeNullOrEmpty()
    }

    @Test
    fun `shouldNotBeNullOrEmpty fails for null`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s.shouldNotBeNullOrEmpty()
        }
    }

    @Test
    fun `shouldNotBeNullOrEmpty fails for empty string`() {
        assertFailsWith<AssertionFailedError> {
            "".shouldNotBeNullOrEmpty()
        }
    }

    @Test
    fun `shouldNotBeNullOrEmpty returns non-null CharSequence`() {
        val s: CharSequence? = "hello"
        val result: CharSequence = s.shouldNotBeNullOrEmpty()
        result.length shouldBeEqualTo 5
    }

    // ── shouldMatch / shouldNotMatch (전체 매치) ──────────────────────────

    @Test
    fun `shouldMatch Regex passes when entire string matches`() {
        "12345" shouldMatch Regex("\\d+")
    }

    @Test
    fun `shouldMatch Regex fails when string does not fully match`() {
        assertFailsWith<AssertionFailedError> {
            // Regex "\\d+" would match only the digits part, not the full string
            "12345abc" shouldMatch Regex("\\d+")
        }
    }

    @Test
    fun `shouldMatch Regex fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldMatch Regex("\\d+")
        }
    }

    @Test
    fun `shouldMatch String pattern passes when entire string matches`() {
        "hello" shouldMatch "[a-z]+"
    }

    @Test
    fun `shouldMatch String pattern fails when string does not fully match`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldMatch "[a-z]+"
        }
    }

    @Test
    fun `shouldNotMatch passes when string does not match regex`() {
        "hello" shouldNotMatch Regex("\\d+")
    }

    @Test
    fun `shouldNotMatch passes for null receiver`() {
        val s: CharSequence? = null
        s shouldNotMatch Regex("\\d+")
    }

    @Test
    fun `shouldNotMatch fails when string fully matches regex`() {
        assertFailsWith<AssertionFailedError> {
            "12345" shouldNotMatch Regex("\\d+")
        }
    }

    // ── shouldContainAll ──────────────────────────────────────────────────

    @Test
    fun `shouldContainAll vararg passes when all substrings are present`() {
        "the quick brown fox".shouldContainAll("quick", "brown", "fox")
    }

    @Test
    fun `shouldContainAll vararg fails when any substring is missing`() {
        assertFailsWith<AssertionFailedError> {
            "the quick brown fox".shouldContainAll("quick", "cat")
        }
    }

    @Test
    fun `shouldContainAll vararg fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s.shouldContainAll("hello")
        }
    }

    @Test
    fun `shouldContainAll Iterable passes when all substrings are present`() {
        val substrings: List<CharSequence> = listOf("quick", "brown", "fox")
        "the quick brown fox" shouldContainAll substrings
    }

    @Test
    fun `shouldContainAll Iterable fails when any substring is missing`() {
        assertFailsWith<AssertionFailedError> {
            val substrings: List<CharSequence> = listOf("quick", "cat")
            "the quick brown fox" shouldContainAll substrings
        }
    }

    // ── shouldContainNone ──────────────────────────────────────────────────

    @Test
    fun `shouldContainNone passes when none of the substrings are present`() {
        "hello world".shouldContainNone("xyz", "abc", "123")
    }

    @Test
    fun `shouldContainNone passes for null receiver`() {
        val s: CharSequence? = null
        s.shouldContainNone("hello", "world")
    }

    @Test
    fun `shouldContainNone fails when any substring is present`() {
        assertFailsWith<AssertionFailedError> {
            "hello world".shouldContainNone("xyz", "world")
        }
    }

    // ── shouldBeEqualToIgnoringCase ───────────────────────────────────────

    @Test
    fun `shouldBeEqualToIgnoringCase passes with same case`() {
        "Hello World" shouldBeEqualToIgnoringCase "Hello World"
    }

    @Test
    fun `shouldBeEqualToIgnoringCase passes with different case`() {
        "Hello World" shouldBeEqualToIgnoringCase "hello world"
    }

    @Test
    fun `shouldBeEqualToIgnoringCase passes with all uppercase`() {
        "HELLO WORLD" shouldBeEqualToIgnoringCase "hello world"
    }

    @Test
    fun `shouldBeEqualToIgnoringCase fails when strings differ beyond case`() {
        assertFailsWith<AssertionFailedError> {
            "hello" shouldBeEqualToIgnoringCase "world"
        }
    }

    @Test
    fun `shouldBeEqualToIgnoringCase fails for null receiver`() {
        assertFailsWith<AssertionFailedError> {
            val s: CharSequence? = null
            s shouldBeEqualToIgnoringCase "hello"
        }
    }

    // ── shouldContainRegex / shouldNotContainRegex (부분 매치) ────────────

    @Test
    fun `shouldContainRegex passes when part of string matches regex`() {
        "hello 123 world" shouldContainRegex Regex("\\d+")
    }

    @Test
    fun `shouldContainRegex passes even without full-string match`() {
        // shouldMatch would fail here (partial match), but shouldContainRegex passes
        "hello 123 world" shouldContainRegex Regex("[a-z]+")
    }

    @Test
    fun `shouldContainRegex fails when no part matches regex`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldContainRegex Regex("\\d+")
        }
    }

    @Test
    fun `shouldNotContainRegex passes when no part of string matches regex`() {
        "hello world" shouldNotContainRegex Regex("\\d+")
    }

    @Test
    fun `shouldNotContainRegex fails when any part of string matches regex`() {
        assertFailsWith<AssertionFailedError> {
            "hello 123 world" shouldNotContainRegex Regex("\\d+")
        }
    }

    // ── shouldMatch vs shouldContainRegex distinction ─────────────────────

    @Test
    fun `shouldMatch requires full string match while shouldContainRegex requires partial match only`() {
        val value = "hello 123 world"

        // shouldContainRegex passes: some digits found somewhere in the string
        value shouldContainRegex Regex("\\d+")

        // shouldMatch would FAIL: "\\d+" does not match the entire "hello 123 world"
        assertFailsWith<AssertionFailedError> {
            value shouldMatch Regex("\\d+")
        }
    }

    @Test
    fun `shouldMatch passes for exact full-string digit-only match`() {
        // Only digits — shouldMatch passes
        "123456" shouldMatch Regex("\\d+")

        // Also passes shouldContainRegex
        "123456" shouldContainRegex Regex("\\d+")
    }

    @Test
    fun `shouldMatch with word boundary pattern demonstration`() {
        // Full match: entire string is lowercase letters
        "hello" shouldMatch Regex("[a-z]+")

        // shouldContainRegex also passes for partial
        "hello 123" shouldContainRegex Regex("[a-z]+")

        // shouldMatch fails when string has non-matching parts
        assertFailsWith<AssertionFailedError> {
            "hello 123" shouldMatch Regex("[a-z]+")
        }
    }

    // ── shouldContainRegex / shouldNotContainRegex (String pattern) ──────

    @Test
    fun `shouldContainRegex with String pattern passes for partial match`() {
        "hello 123 world" shouldContainRegex "\\d+"
    }

    @Test
    fun `shouldContainRegex with String pattern fails when no match`() {
        assertFailsWith<AssertionFailedError> {
            "hello world" shouldContainRegex "\\d+"
        }
    }

    @Test
    fun `shouldNotContainRegex with String pattern passes when no match`() {
        "hello world" shouldNotContainRegex "\\d+"
    }

    @Test
    fun `shouldNotContainRegex with String pattern fails when pattern matches`() {
        assertFailsWith<AssertionFailedError> {
            "hello 123 world" shouldNotContainRegex "\\d+"
        }
    }

    // ── Chaining verification ─────────────────────────────────────────────

    @Test
    fun `chaining multiple CharSequence assertions`() {
        "hello world"
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldNotBeBlank()
            .shouldStartWith("hello")
            .shouldEndWith("world")
            .shouldContain("lo wo")
            .shouldContainIgnoringCase("HELLO")
    }
}
