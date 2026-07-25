package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class RequireSupportTest {

    companion object: KLogging()

    @Test
    fun `assertion status flags`() {
        RequireSupportTest::class.java.classLoader.setClassAssertionStatus(
            RequireSupportTest::class.qualifiedName, false)
        RequireSupportTest::class.java.desiredAssertionStatus().shouldBeFalse()

        class TestClass
        TestClass::class.java.classLoader.setClassAssertionStatus(TestClass::class.qualifiedName, true)
        TestClass::class.java.desiredAssertionStatus().shouldBeTrue()
    }

    @Test
    fun `require null and not-null checks`() {
        var x: Long? = null
        shouldFailRequire { x.requireNotNull("x") }
        x = 12L; x.requireNotNull("x")

        val s: String? = null
        s.requireNull("x")
        shouldFailRequire { "hello".requireNull("x") }
    }

    @Test
    fun `require string emptiness and blankness`() {
        var x: String? = null
        shouldFailRequire { x.requireNotEmpty("x") }
        x = ""; shouldFailRequire { x.requireNotEmpty("x") }
        x = "    "; x.requireNotEmpty("x")
        x = "  \t "; x.requireNotEmpty("x")

        x = null; shouldFailRequire { x.requireNotBlank("x") }
        x = ""; shouldFailRequire { x.requireNotBlank("x") }
        x = "    "; shouldFailRequire { x.requireNotBlank("x") }
        x = "  \t "; shouldFailRequire { x.requireNotBlank("x") }
    }

    @Test
    fun `require null-or-empty and null-or-blank`() {
        val empty: String? = null
        empty.requireNullOrEmpty("x"); "".requireNullOrEmpty("x")
        shouldFailRequire { "hello".requireNullOrEmpty("x") }

        empty.requireNullOrBlank("x"); "".requireNullOrBlank("x"); "   ".requireNullOrBlank("x")
        shouldFailRequire { "hello".requireNullOrBlank("x") }
    }

    @Test
    fun `require string contains startsWith endsWith`() {
        "hello world".requireContains("world", "x")
        shouldFailRequire { "hello".requireContains("world", "x") }

        "hello world".requireStartsWith("hello", "x")
        shouldFailRequire { "hello world".requireStartsWith("world", "x") }
        "Hello World".requireStartsWith("hello", "x", ignoreCase = true)

        "hello world".requireEndsWith("world", "x")
        shouldFailRequire { "hello world".requireEndsWith("hello", "x") }
        "Hello World".requireEndsWith("WORLD", "x", ignoreCase = true)
    }

    @Test
    fun `require comparable ordering and equality`() {
        42.requireEquals(42, "x"); shouldFailRequire { 42.requireEquals(99, "x") }

        10.requireGt(5, "x")
        shouldFailRequire { 5.requireGt(10, "x") }; shouldFailRequire { 5.requireGt(5, "x") }

        10.requireGe(5, "x"); 5.requireGe(5, "x")
        shouldFailRequire { 4.requireGe(5, "x") }

        5.requireLt(10, "x")
        shouldFailRequire { 10.requireLt(5, "x") }; shouldFailRequire { 5.requireLt(5, "x") }

        5.requireLe(10, "x"); 5.requireLe(5, "x")
        shouldFailRequire { 6.requireLe(5, "x") }
    }

    @Test
    fun `require in range and in open range`() {
        5.requireInRange(1, 10, "x"); 1.requireInRange(1, 10, "x"); 10.requireInRange(1, 10, "x")
        shouldFailRequire { 0.requireInRange(1, 10, "x") }; shouldFailRequire { 11.requireInRange(1, 10, "x") }

        5.requireInOpenRange(1, 10, "x"); 1.requireInOpenRange(1, 10, "x")
        shouldFailRequire { 10.requireInOpenRange(1, 10, "x") }
    }

    @Test
    fun `require number sign variants`() {
        1.requirePositiveNumber("x"); 0.1.requirePositiveNumber("x")
        shouldFailRequire { 0.requirePositiveNumber("x") }; shouldFailRequire { (-1).requirePositiveNumber("x") }

        0.requireZeroOrPositiveNumber("x"); 1.requireZeroOrPositiveNumber("x")
        shouldFailRequire { (-1).requireZeroOrPositiveNumber("x") }

        (-1).requireNegativeNumber("x")
        shouldFailRequire { 0.requireNegativeNumber("x") }; shouldFailRequire { 1.requireNegativeNumber("x") }

        0.requireZeroOrNegativeNumber("x"); (-1).requireZeroOrNegativeNumber("x")
        shouldFailRequire { 1.requireZeroOrNegativeNumber("x") }
    }

    @Test
    fun `require collection and array not empty`() {
        val array = arrayOf(1, 2, 3)
        (array.requireNotEmpty("x") === array).shouldBeTrue()
        shouldFailRequire { emptyArray<Int>().requireNotEmpty("x") }
        shouldFailRequire { (null as Array<Int>?).requireNotEmpty("x") }

        val list = listOf(1, 2, 3)
        (list.requireNotEmpty("x") === list).shouldBeTrue()
        shouldFailRequire { emptyList<Int>().requireNotEmpty("x") }
        shouldFailRequire { (null as List<Int>?).requireNotEmpty("x") }
    }

    @Test
    fun `require not empty returns non-null collection types`() {
        val nullableArray: Array<Int>? = arrayOf(1, 2)
        val array: Array<Int> = nullableArray.requireNotEmpty("items")
        array.size.shouldBeEqualTo(2)

        val nullableCollection: Collection<Int>? = listOf(1, 2)
        val collection: Collection<Int> = nullableCollection.requireNotEmpty("items")
        collection.size.shouldBeEqualTo(2)

        val nullableMap: Map<String, Int>? = mapOf("one" to 1)
        val map: Map<String, Int> = nullableMap.requireNotEmpty("items")
        map.size.shouldBeEqualTo(1)
    }

    @Test
    fun `require map operations`() {
        val map = mapOf("a" to 1)
        (map.requireNotEmpty("x") === map).shouldBeTrue()
        shouldFailRequire { emptyMap<String, Int>().requireNotEmpty("x") }
        shouldFailRequire { (null as Map<String, Int>?).requireNotEmpty("x") }

        mapOf("a" to 1, "b" to 2).requireHasKey("a", "x")
        shouldFailRequire { mapOf("a" to 1).requireHasKey("b", "x") }
        shouldFailRequire { (null as Map<String, Int>?).requireHasKey("a", "x") }

        mapOf("a" to 1, "b" to 2).requireHasValue(1, "x")
        shouldFailRequire { mapOf("a" to 1).requireHasValue(99, "x") }
        shouldFailRequire { (null as Map<String, Int>?).requireHasValue(1, "x") }

        mapOf("a" to 1, "b" to 2).requireContains("a", 1, "x")
        shouldFailRequire { mapOf("a" to 1).requireContains("a", 99, "x") }
        shouldFailRequire { (null as Map<String, Int>?).requireContains("a", 1, "x") }
    }
}
