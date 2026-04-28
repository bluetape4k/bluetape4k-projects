package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class AssertSupportTest {

    companion object: KLogging()

    @Test
    fun `assert null and not-null checks`() {
        var x: Long? = null
        shouldFailRequire { x.assertNotNull("x").toByteArray() }
        x = 12L; x.assertNotNull("x")

        val s: String? = null
        s.assertNull("x")
        shouldFailRequire { "hello".assertNull("x") }
    }

    @Test
    fun `assert string emptiness and blankness`() {
        var x: String? = null
        shouldFailRequire { x.assertNotEmpty("x") }
        x = ""; shouldFailRequire { x.assertNotEmpty("x") }
        x = "    "; x.assertNotEmpty("x")
        x = "  \t "; x.assertNotEmpty("x")

        x = null; shouldFailRequire { x.assertNotBlank("x") }
        x = ""; shouldFailRequire { x.assertNotBlank("x") }
        x = "    "; shouldFailRequire { x.assertNotBlank("x") }
        x = "  \t "; shouldFailRequire { x.assertNotBlank("x") }
    }

    @Test
    fun `assert null-or-empty and null-or-blank`() {
        val empty: String? = null
        empty.assertNullOrEmpty("x"); "".assertNullOrEmpty("x")
        shouldFailRequire { "hello".assertNullOrEmpty("x") }

        empty.assertNullOrBlank("x"); "".assertNullOrBlank("x"); "   ".assertNullOrBlank("x")
        shouldFailRequire { "hello".assertNullOrBlank("x") }
    }

    @Test
    fun `assert string contains startsWith endsWith`() {
        "hello world".assertContains("world", "x")
        shouldFailRequire { "hello".assertContains("world", "x") }

        "hello world".assertStartsWith("hello", "x")
        shouldFailRequire { "hello world".assertStartsWith("world", "x") }
        "Hello World".assertStartsWith("hello", "x", ignoreCase = true)

        "hello world".assertEndsWith("world", "x")
        shouldFailRequire { "hello world".assertEndsWith("hello", "x") }
        "Hello World".assertEndsWith("WORLD", "x", ignoreCase = true)
    }

    @Test
    fun `assert comparable ordering and equality`() {
        42.assertEquals(42, "x"); shouldFailRequire { 42.assertEquals(99, "x") }

        10.assertGt(5, "x")
        shouldFailRequire { 5.assertGt(10, "x") }; shouldFailRequire { 5.assertGt(5, "x") }

        10.assertGe(5, "x"); 5.assertGe(5, "x")
        shouldFailRequire { 4.assertGe(5, "x") }

        5.assertLt(10, "x")
        shouldFailRequire { 10.assertLt(5, "x") }; shouldFailRequire { 5.assertLt(5, "x") }

        5.assertLe(10, "x"); 5.assertLe(5, "x")
        shouldFailRequire { 6.assertLe(5, "x") }
    }

    @Test
    fun `assert in range and in open range`() {
        5.assertInRange(1, 10, "x"); 1.assertInRange(1, 10, "x"); 10.assertInRange(1, 10, "x")
        shouldFailRequire { 0.assertInRange(1, 10, "x") }; shouldFailRequire { 11.assertInRange(1, 10, "x") }

        5.assertInOpenRange(1, 10, "x"); 1.assertInOpenRange(1, 10, "x")
        shouldFailRequire { 10.assertInOpenRange(1, 10, "x") }
    }

    @Test
    fun `assert number sign variants`() {
        1.assertPositiveNumber("x"); 0.1.assertPositiveNumber("x")
        shouldFailRequire { 0.assertPositiveNumber("x") }; shouldFailRequire { (-1).assertPositiveNumber("x") }

        0.assertZeroOrPositiveNumber("x"); 1.assertZeroOrPositiveNumber("x")
        shouldFailRequire { (-1).assertZeroOrPositiveNumber("x") }

        (-1).assertNegativeNumber("x")
        shouldFailRequire { 0.assertNegativeNumber("x") }; shouldFailRequire { 1.assertNegativeNumber("x") }

        0.assertZeroOrNegativeNumber("x"); (-1).assertZeroOrNegativeNumber("x")
        shouldFailRequire { 1.assertZeroOrNegativeNumber("x") }
    }

    @Test
    fun `assert collection and map not empty`() {
        listOf(1, 2, 3).assertNotEmpty("x")
        shouldFailRequire { emptyList<Int>().assertNotEmpty("x") }
        shouldFailRequire { (null as List<Int>?).assertNotEmpty("x") }

        mapOf("a" to 1).assertNotEmpty("x")
        shouldFailRequire { emptyMap<String, Int>().assertNotEmpty("x") }
        shouldFailRequire { (null as Map<String, Int>?).assertNotEmpty("x") }
    }

    @Test
    fun `assert map key and value operations`() {
        mapOf("a" to 1, "b" to 2).assertHasKey("a", "x")
        shouldFailRequire { mapOf("a" to 1).assertHasKey("b", "x") }

        mapOf("a" to 1, "b" to 2).assertHasValue(1, "x")
        shouldFailRequire { mapOf("a" to 1).assertHasValue(99, "x") }

        mapOf("a" to 1, "b" to 2).assertContains("a", 1, "x")
        shouldFailRequire { mapOf("a" to 1).assertContains("a", 99, "x") }
    }

    @Test
    fun `require 기반으로 항상 검증된다 (JVM -ea 플래그 불필요)`() {
        // assert() 와 달리 require() 는 JVM 플래그 없이도 항상 실행됨을 확인
        shouldFailRequire { (null as String?).assertNotNull("x") }
        shouldFailRequire { "".assertNotEmpty("x") }
        shouldFailRequire { "  ".assertNotBlank("x") }
    }
}
