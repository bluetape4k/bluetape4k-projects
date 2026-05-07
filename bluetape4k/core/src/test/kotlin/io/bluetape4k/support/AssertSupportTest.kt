package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

@Suppress("DEPRECATION")
class AssertSupportTest {

    companion object: KLogging()

    @Test
    fun `assert null and not-null checks`() {
        var x: Long? = null
        shouldFailAssert { x.assertNotNull("x").toByteArray() }
        x = 12L; x.assertNotNull("x")

        val s: String? = null
        s.assertNull("x")
        shouldFailAssert { "hello".assertNull("x") }
    }

    @Test
    fun `assert string emptiness and blankness`() {
        var x: String? = null
        shouldFailAssert { x.assertNotEmpty("x") }
        x = ""; shouldFailAssert { x.assertNotEmpty("x") }
        x = "    "; x.assertNotEmpty("x")
        x = "  \t "; x.assertNotEmpty("x")

        x = null; shouldFailAssert { x.assertNotBlank("x") }
        x = ""; shouldFailAssert { x.assertNotBlank("x") }
        x = "    "; shouldFailAssert { x.assertNotBlank("x") }
        x = "  \t "; shouldFailAssert { x.assertNotBlank("x") }
    }

    @Test
    fun `assert null-or-empty and null-or-blank`() {
        val empty: String? = null
        empty.assertNullOrEmpty("x"); "".assertNullOrEmpty("x")
        shouldFailAssert { "hello".assertNullOrEmpty("x") }

        empty.assertNullOrBlank("x"); "".assertNullOrBlank("x"); "   ".assertNullOrBlank("x")
        shouldFailAssert { "hello".assertNullOrBlank("x") }
    }

    @Test
    fun `assert string contains startsWith endsWith`() {
        "hello world".assertContains("world", "x")
        shouldFailAssert { "hello".assertContains("world", "x") }

        "hello world".assertStartsWith("hello", "x")
        shouldFailAssert { "hello world".assertStartsWith("world", "x") }
        "Hello World".assertStartsWith("hello", "x", ignoreCase = true)

        "hello world".assertEndsWith("world", "x")
        shouldFailAssert { "hello world".assertEndsWith("hello", "x") }
        "Hello World".assertEndsWith("WORLD", "x", ignoreCase = true)
    }

    @Test
    fun `assert comparable ordering and equality`() {
        42.assertEquals(42, "x"); shouldFailAssert { 42.assertEquals(99, "x") }

        10.assertGt(5, "x")
        shouldFailAssert { 5.assertGt(10, "x") }; shouldFailAssert { 5.assertGt(5, "x") }

        10.assertGe(5, "x"); 5.assertGe(5, "x")
        shouldFailAssert { 4.assertGe(5, "x") }

        5.assertLt(10, "x")
        shouldFailAssert { 10.assertLt(5, "x") }; shouldFailAssert { 5.assertLt(5, "x") }

        5.assertLe(10, "x"); 5.assertLe(5, "x")
        shouldFailAssert { 6.assertLe(5, "x") }
    }

    @Test
    fun `assert in range and in open range`() {
        5.assertInRange(1, 10, "x"); 1.assertInRange(1, 10, "x"); 10.assertInRange(1, 10, "x")
        shouldFailAssert { 0.assertInRange(1, 10, "x") }; shouldFailAssert { 11.assertInRange(1, 10, "x") }

        5.assertInOpenRange(1, 10, "x"); 1.assertInOpenRange(1, 10, "x")
        shouldFailAssert { 10.assertInOpenRange(1, 10, "x") }
    }

    @Test
    fun `assert number sign variants`() {
        1.assertPositiveNumber("x"); 0.1.assertPositiveNumber("x")
        shouldFailAssert { 0.assertPositiveNumber("x") }; shouldFailAssert { (-1).assertPositiveNumber("x") }

        0.assertZeroOrPositiveNumber("x"); 1.assertZeroOrPositiveNumber("x")
        shouldFailAssert { (-1).assertZeroOrPositiveNumber("x") }

        (-1).assertNegativeNumber("x")
        shouldFailAssert { 0.assertNegativeNumber("x") }; shouldFailAssert { 1.assertNegativeNumber("x") }

        0.assertZeroOrNegativeNumber("x"); (-1).assertZeroOrNegativeNumber("x")
        shouldFailAssert { 1.assertZeroOrNegativeNumber("x") }
    }

    @Test
    fun `assert collection and map not empty`() {
        listOf(1, 2, 3).assertNotEmpty("x")
        shouldFailAssert { emptyList<Int>().assertNotEmpty("x") }
        shouldFailAssert { (null as List<Int>?).assertNotEmpty("x") }

        mapOf("a" to 1).assertNotEmpty("x")
        shouldFailAssert { emptyMap<String, Int>().assertNotEmpty("x") }
        shouldFailAssert { (null as Map<String, Int>?).assertNotEmpty("x") }
    }

    @Test
    fun `assert map key and value operations`() {
        mapOf("a" to 1, "b" to 2).assertHasKey("a", "x")
        shouldFailAssert { mapOf("a" to 1).assertHasKey("b", "x") }

        mapOf("a" to 1, "b" to 2).assertHasValue(1, "x")
        shouldFailAssert { mapOf("a" to 1).assertHasValue(99, "x") }

        mapOf("a" to 1, "b" to 2).assertContains("a", 1, "x")
        shouldFailAssert { mapOf("a" to 1).assertContains("a", 99, "x") }
    }

    /**
     * 이 테스트가 실패하면 AssertSupport.kt의 예외 타입이 잘못 변경된 것입니다.
     * assertXxx() = AssertionError, requireXxx() = IllegalArgumentException — 절대 혼용 금지.
     */
    @Test
    fun `assertXxx는 AssertionError를 발생시킨다`() {
        shouldFailAssert { (null as String?).assertNotNull("x") }
        shouldFailAssert { "".assertNotEmpty("x") }
        shouldFailAssert { "  ".assertNotBlank("x") }
    }

    @Test
    fun `assertXxx는 IllegalArgumentException이 아닌 AssertionError를 던진다 - 예외 타입 계약 고정`() {
        // 이 테스트는 AssertSupport.kt의 예외 타입 계약을 고정합니다.
        // require()로 바꾸면 즉시 실패 → cross-module 회귀 전에 여기서 먼저 감지됩니다.
        assertFailsWith<AssertionError> { (null as String?).assertNotNull("x") }
        assertFailsWith<AssertionError> { "".assertNotBlank("x") }
        assertFailsWith<AssertionError> { (-1).assertPositiveNumber("x") }
        assertFailsWith<AssertionError> { 0.assertPositiveNumber("x") }
        assertFailsWith<AssertionError> { 11.assertInRange(1, 10, "x") }
    }
}
