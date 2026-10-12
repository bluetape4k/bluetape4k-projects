package io.bluetape4k.support

import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldFailRequire
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class RequireSupportTest {

    companion object: KLogging()

    @Test
    fun `assertion status flags`() {
        RequireSupportTest::class.java.classLoader.setClassAssertionStatus(
            RequireSupportTest::class.qualifiedName, false
        )
        RequireSupportTest::class.java.desiredAssertionStatus().shouldBeFalse()

        class TestClass
        TestClass::class.java.classLoader.setClassAssertionStatus(TestClass::class.qualifiedName, true)
        TestClass::class.java.desiredAssertionStatus().shouldBeTrue()
    }

    @Test
    fun `require null and not-null checks`() {
        var x: Long? = null
        shouldFailRequire { x.requireNotNull("x") }

        x = 12L
        x.requireNotNull("x")

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

        val array: Array<Int>? = emptyArray()
        array.requireNullOrEmpty("items")
        val collection: Collection<Int>? = emptyList()
        collection.requireNullOrEmpty("items")
        val map: Map<String, Int>? = emptyMap()
        map.requireNullOrEmpty("items")
        shouldFailRequire { arrayOf(1).requireNullOrEmpty("items") }
        shouldFailRequire { listOf(1).requireNullOrEmpty("items") }
        shouldFailRequire { mapOf("a" to 1).requireNullOrEmpty("items") }
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
        "hello world".requireEndsWith("world") { "unexpected suffix mismatch" }
        shouldFailRequire { (null as String?).requireEndsWith("world", "x") }
        shouldFailRequire { (null as String?).requireEndsWith("world") { "missing text" } }
    }

    @Test
    fun `require bounded lengths and sizes`() {
        val text: String? = "blue"

        text.requireLengthInRange(4, 4, "text") shouldBe text
        shouldFailRequire { "".requireLengthInRange(1, 4, "text") }
        shouldFailRequire { "bluebird".requireLengthInRange(1, 4, "text") }
        shouldFailRequire { (null as String?).requireLengthInRange(1, 4, "text") }

        val array = arrayOf(1, 2)
        array.requireSizeInRange(1, 2, "items") shouldBe array
        shouldFailRequire { emptyArray<Int>().requireSizeInRange(1, 2, "items") }
        shouldFailRequire { (null as Array<Int>?).requireSizeInRange(1, 2, "items") }

        val collection = listOf(1, 2)
        collection.requireSizeInRange(1, 2, "items") shouldBe collection
        shouldFailRequire { listOf(1, 2, 3).requireSizeInRange(1, 2, "items") }

        val map = mapOf("one" to 1)
        map.requireSizeInRange(1, 2, "items") shouldBe map
        shouldFailRequire { emptyMap<String, Int>().requireSizeInRange(1, 2, "items") }
    }

    @Test
    fun `require regex and finite checks`() {
        val value: String? = "SKU-42"
        value.requireMatches(Regex("SKU-\\d+"), "sku") shouldBe value
        val failure = shouldFailRequire { "secret".requireMatches(Regex("SKU-\\d+"), "sku") }
        failure.message.orEmpty().contains("secret").shouldBeFalse()

        1.0f.requireFinite("ratio")
        Double.MAX_VALUE.requireFinite("ratio")
        shouldFailRequire { Float.NaN.requireFinite("ratio") }
        shouldFailRequire { Double.POSITIVE_INFINITY.requireFinite("ratio") }
    }

    @Test
    fun `require validation messages are lazy and customizable`() {
        var evaluated = false
        "blue".requireLengthInRange(1, 4, "text") {
            evaluated = true
            "custom"
        }
        evaluated.shouldBeFalse()

        val failure = shouldFailRequire {
            "too long".requireLengthInRange(1, 4, "text") {
                evaluated = true
                "custom length"
            }
        }
        evaluated.shouldBeTrue()
        failure.message.shouldBeEqualTo("custom length")
    }

    @Test
    fun `require comparable ordering and equality`() {
        42.requireEquals(42, "x")
        shouldFailRequire { 42.requireEquals(99, "x") }

        10.requireGt(5, "x")
        shouldFailRequire { 5.requireGt(10, "x") }
        shouldFailRequire { 5.requireGt(5, "x") }

        10.requireGe(5, "x")
        5.requireGe(5, "x")
        shouldFailRequire { 4.requireGe(5, "x") }

        5.requireLt(10, "x")
        shouldFailRequire { 10.requireLt(5, "x") }
        shouldFailRequire { 5.requireLt(5, "x") }

        5.requireLe(10, "x")
        5.requireLe(5, "x")
        shouldFailRequire { 6.requireLe(5, "x") }
    }

    @Test
    fun `require in range and in open range`() {
        5.requireInRange(1, 10, "x")
        1.requireInRange(1, 10, "x")
        10.requireInRange(1, 10, "x")
        shouldFailRequire { 0.requireInRange(1, 10, "x") }
        shouldFailRequire { 11.requireInRange(1, 10, "x") }

        5.requireInOpenRange(1, 10, "x")
        1.requireInOpenRange(1, 10, "x")
        shouldFailRequire { 10.requireInOpenRange(1, 10, "x") }
    }

    @Test
    fun `require number sign variants`() {
        1.requirePositiveNumber("x")
        0.1.requirePositiveNumber("x")
        shouldFailRequire { 0.requirePositiveNumber("x") }
        shouldFailRequire { (-1).requirePositiveNumber("x") }

        0.requireZeroOrPositiveNumber("x")
        1.requireZeroOrPositiveNumber("x")
        shouldFailRequire { (-1).requireZeroOrPositiveNumber("x") }

        (-1).requireNegativeNumber("x")
        shouldFailRequire { 0.requireNegativeNumber("x") }
        shouldFailRequire { 1.requireNegativeNumber("x") }

        0.requireZeroOrNegativeNumber("x")
        (-1).requireZeroOrNegativeNumber("x")
        shouldFailRequire { 1.requireZeroOrNegativeNumber("x") }
    }

    @Test
    fun `require collection and array not empty`() {
        val array = arrayOf(1, 2, 3)
        array.requireNotEmpty("x") shouldBe array
        shouldFailRequire { emptyArray<Int>().requireNotEmpty("x") }
        shouldFailRequire { (null as Array<Int>?).requireNotEmpty("x") }

        val list = listOf(1, 2, 3)
        list.requireNotEmpty("x") shouldBe list
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
        map.requireNotEmpty("x") shouldBe map
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

    @Test
    fun `require negative helpers complement the selected positive helpers`() {
        41.requireNotEquals(42, "x")
        shouldFailRequire { 42.requireNotEquals(42, "x") }

        "hello".requireNotContains("world", "x")
        "hello".requireNotStartsWith("world", "x")
        "hello".requireNotEndsWith("world", "x")
        shouldFailRequire { "hello".requireNotContains("ell", "x") }
        shouldFailRequire { "hello".requireNotStartsWith("he", "x") }
        shouldFailRequire { "hello".requireNotEndsWith("llo", "x") }
        "Hello".requireNotStartsWith("WORLD", "x", ignoreCase = true)
        "Hello".requireNotEndsWith("WORLD", "x", ignoreCase = true)

        shouldFailRequire { 5.requireNotInRange(1, 10, "x") }
        0.requireNotInRange(1, 10, "x")
        10.requireNotInOpenRange(1, 10, "x")
        shouldFailRequire { 5.requireNotInOpenRange(1, 10, "x") }
        0.requireNotInRange(1, 10) { "unused" }
        10.requireNotInOpenRange(1, 10) { "unused" }

        shouldFailRequire { "blue".requireNotLengthInRange(1, 4, "text") }
        "".requireNotLengthInRange(1, 4, "text")
        shouldFailRequire { listOf(1, 2).requireNotSizeInRange(1, 3, "items") }
        emptyList<Int>().requireNotSizeInRange(1, 3, "items")
        shouldFailRequire { mapOf("a" to 1).requireNotSizeInRange(1, 2, "items") }
        emptyMap<String, Int>().requireNotSizeInRange(1, 2, "items")
        shouldFailRequire { arrayOf(1).requireNotSizeInRange(1, 2, "items") }
        emptyArray<Int>().requireNotSizeInRange(1, 2, "items")

        mapOf("a" to 1).requireNotHasKey("b", "map")
        mapOf("a" to 1).requireNotHasValue(2, "map")
        mapOf("a" to 1).requireNotContains("a", 2, "map")
        emptyMap<String, Int>().requireNotHasKey("a", "map")
        shouldFailRequire { mapOf("a" to 1).requireNotHasKey("a", "map") }
        shouldFailRequire { mapOf("a" to 1).requireNotHasValue(1, "map") }
        shouldFailRequire { mapOf("a" to 1).requireNotContains("a", 1, "map") }
        shouldFailRequire { (null as Map<String, Int>?).requireNotHasKey("a", "map") }

        Float.NaN.requireNotFinite("value")
        Double.POSITIVE_INFINITY.requireNotFinite("value")
        shouldFailRequire { 0.0.requireNotFinite("value") }
        shouldFailRequire { 1.0f.requireNotFinite("value") }

        1.0f.requireFinitePositiveNumber("value")
        0.0f.requireFiniteZeroOrNegativeNumber("value")
        0.0.requireFiniteZeroOrPositiveNumber("value")
        (-1.0).requireFiniteNegativeNumber("value")
        shouldFailRequire { Float.NaN.requireFinitePositiveNumber("value") }
        shouldFailRequire { Float.POSITIVE_INFINITY.requireFiniteZeroOrNegativeNumber("value") }
        shouldFailRequire { Double.NEGATIVE_INFINITY.requireFiniteNegativeNumber("value") }
        shouldFailRequire { (-1.0).requireFiniteZeroOrPositiveNumber("value") }
    }

    @Test
    fun `require negative helpers preserve callbacks, empty ranges, and map membership semantics`() {
        var callbackCount = 0
        0.requireNotInRange(1, 10) {
            callbackCount++
            "unused"
        }
        10.requireNotInOpenRange(1, 10) {
            callbackCount++
            "unused"
        }
        callbackCount shouldBeEqualTo 0

        val sentinel = IllegalArgumentException("sentinel")
        var thrown: IllegalArgumentException? = null
        try {
            "hello".requireNotContains("ell") {
                callbackCount++
                throw sentinel
            }
        } catch (error: IllegalArgumentException) {
            thrown = error
        }
        thrown shouldBe sentinel
        callbackCount shouldBeEqualTo 1

        val nullableValueMap = mapOf("present" to null)
        nullableValueMap.requireContains("present", null, "map")
        nullableValueMap.requireNotContains("missing", null, "map")
        shouldFailRequire { nullableValueMap.requireContains("missing", null, "map") }
        shouldFailRequire { nullableValueMap.requireNotContains("present", null, "map") }
        (emptyMap<String, Int?>()).requireNotContains("missing", null, "map")
    }
}
