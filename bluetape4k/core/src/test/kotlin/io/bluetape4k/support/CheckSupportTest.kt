package io.bluetape4k.support

import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class CheckSupportTest {

    companion object: KLogging()

    @Test
    fun `check null and not-null state`() {
        val value: String? = "blue"
        (value.checkNotNull("value") === value).shouldBeTrue()

        val empty: String? = null
        (empty.checkNull("value") === empty).shouldBeTrue()
        shouldFailCheck { empty.checkNotNull("value") }.message shouldBeEqualTo
                "value[null] must not be null."
        shouldFailCheck { value.checkNull("value") }
    }

    @Test
    fun `check string emptiness and blankness`() {
        val value: String? = "blue"
        value.checkNotEmpty("value") shouldBe value
        value.checkNotBlank("value") shouldBe value

        shouldFailCheck { (null as String?).checkNotEmpty("value") }
        shouldFailCheck { "".checkNotEmpty("value") }
        shouldFailCheck { (null as String?).checkNotBlank("value") }
        shouldFailCheck { "   ".checkNotBlank("value") }
    }

    @Test
    fun `check null-or-empty and null-or-blank state`() {
        val empty: String? = null
        empty.checkNullOrEmpty("value")
        "".checkNullOrEmpty("value")
        empty.checkNullOrBlank("value")
        "   ".checkNullOrBlank("value")

        shouldFailCheck { "blue".checkNullOrEmpty("value") }
        shouldFailCheck { "blue".checkNullOrBlank("value") }
    }

    @Test
    fun `check string contains startsWith endsWith`() {
        val value: String? = "Hello World"
        (value.checkContains("World", "value") === value).shouldBeTrue()
        (value.checkStartsWith("hello", "value", ignoreCase = true) === value).shouldBeTrue()
        (value.checkEndsWith("WORLD", "value", ignoreCase = true) === value).shouldBeTrue()

        shouldFailCheck { "hello".checkContains("world", "value") }
        shouldFailCheck { "hello world".checkStartsWith("world", "value") }
        shouldFailCheck { "hello world".checkEndsWith("hello", "value") }
    }

    @Test
    fun `check bounded lengths and sizes`() {
        val text: String? = "blue"
        (text.checkLengthInRange(4, 4, "text") === text).shouldBeTrue()
        shouldFailCheck { "".checkLengthInRange(1, 4, "text") }
        shouldFailCheck { (null as String?).checkLengthInRange(1, 4, "text") }

        val array = arrayOf(1, 2)
        (array.checkSizeInRange(1, 2, "items") === array).shouldBeTrue()
        shouldFailCheck { emptyArray<Int>().checkSizeInRange(1, 2, "items") }
        shouldFailCheck { (null as Array<Int>?).checkSizeInRange(1, 2, "items") }

        val collection = listOf(1, 2)
        (collection.checkSizeInRange(1, 2, "items") === collection).shouldBeTrue()
        shouldFailCheck { listOf(1, 2, 3).checkSizeInRange(1, 2, "items") }

        val map = mapOf("one" to 1)
        (map.checkSizeInRange(1, 2, "items") === map).shouldBeTrue()
        shouldFailCheck { emptyMap<String, Int>().checkSizeInRange(1, 2, "items") }
    }

    @Test
    fun `check regex and finite checks`() {
        val value: String? = "SKU-42"
        (value.checkMatches(Regex("SKU-\\d+"), "sku") === value).shouldBeTrue()
        val failure = shouldFailCheck { "secret".checkMatches(Regex("SKU-\\d+"), "sku") }
        failure.message.orEmpty().contains("secret").shouldBeFalse()

        1.0f.checkFinite("ratio") shouldBeEqualTo 1.0f
        Double.MAX_VALUE.checkFinite("ratio") shouldBeEqualTo Double.MAX_VALUE
        shouldFailCheck { Float.NaN.checkFinite("ratio") }
        shouldFailCheck { Double.NEGATIVE_INFINITY.checkFinite("ratio") }
    }

    @Test
    fun `check validation messages are lazy and customizable`() {
        var evaluated = false
        "blue".checkLengthInRange(1, 4, "text") {
            evaluated = true
            "custom"
        }
        evaluated.shouldBeFalse()

        val failure = shouldFailCheck {
            "too long".checkLengthInRange(1, 4, "text") {
                evaluated = true
                "custom length"
            }
        }
        evaluated.shouldBeTrue()
        failure.message.shouldBeEqualTo("custom length")
    }

    @Test
    fun `check comparable ordering and equality`() {
        42.checkEquals(42, "value") shouldBeEqualTo 42
        10.checkGt(5, "value") shouldBeEqualTo 10
        5.checkGe(5, "value") shouldBeEqualTo 5
        5.checkLt(10, "value") shouldBeEqualTo 5
        5.checkLe(5, "value") shouldBeEqualTo 5

        shouldFailCheck { 42.checkEquals(99, "value") }
        shouldFailCheck { 5.checkGt(5, "value") }
        shouldFailCheck { 4.checkGe(5, "value") }
        shouldFailCheck { 5.checkLt(5, "value") }
        shouldFailCheck { 6.checkLe(5, "value") }
    }

    @Test
    fun `check closed and open ranges`() {
        1.checkInRange(1, 10, "value") shouldBeEqualTo 1
        10.checkInRange(1, 10, "value") shouldBeEqualTo 10
        1.checkInOpenRange(1, 10, "value") shouldBeEqualTo 1

        shouldFailCheck { 0.checkInRange(1, 10, "value") }
        shouldFailCheck { 11.checkInRange(1, 10, "value") }
        shouldFailCheck { 10.checkInOpenRange(1, 10, "value") }
    }

    @Test
    fun `check number sign variants`() {
        1.checkPositiveNumber("value") shouldBeEqualTo 1
        0.checkZeroOrPositiveNumber("value") shouldBeEqualTo 0
        (-1).checkNegativeNumber("value") shouldBeEqualTo -1
        0.checkZeroOrNegativeNumber("value") shouldBeEqualTo 0

        shouldFailCheck { 0.checkPositiveNumber("value") }
        shouldFailCheck { (-1).checkZeroOrPositiveNumber("value") }
        shouldFailCheck { 0.checkNegativeNumber("value") }
        shouldFailCheck { 1.checkZeroOrNegativeNumber("value") }
    }

    @Test
    fun `check collection and array not empty`() {
        val array = arrayOf(1, 2)
        (array.checkNotEmpty("items") === array).shouldBeTrue()
        val list = listOf(1, 2)
        (list.checkNotEmpty("items") === list).shouldBeTrue()

        shouldFailCheck { emptyArray<Int>().checkNotEmpty("items") }
        shouldFailCheck { (null as Array<Int>?).checkNotEmpty("items") }
        shouldFailCheck { emptyList<Int>().checkNotEmpty("items") }
        shouldFailCheck { (null as List<Int>?).checkNotEmpty("items") }
    }

    @Test
    fun `check not empty returns non-null collection types`() {
        val nullableArray: Array<Int>? = arrayOf(1, 2)
        val array: Array<Int> = nullableArray.checkNotEmpty("items")
        array.size.shouldBeEqualTo(2)

        val nullableCollection: Collection<Int>? = listOf(1, 2)
        val collection: Collection<Int> = nullableCollection.checkNotEmpty("items")
        collection.size.shouldBeEqualTo(2)

        val nullableMap: Map<String, Int>? = mapOf("one" to 1)
        val map: Map<String, Int> = nullableMap.checkNotEmpty("items")
        map.size.shouldBeEqualTo(1)
    }

    @Test
    fun `check map operations`() {
        val map = mapOf("a" to 1, "b" to 2)
        (map.checkNotEmpty("map") === map).shouldBeTrue()
        (map.checkHasKey("a", "map") === map).shouldBeTrue()
        (map.checkHasValue(1, "map") === map).shouldBeTrue()
        (map.checkContains("a", 1, "map") === map).shouldBeTrue()

        shouldFailCheck { emptyMap<String, Int>().checkNotEmpty("map") }
        shouldFailCheck { (null as Map<String, Int>?).checkNotEmpty("map") }
        shouldFailCheck { map.checkHasKey("missing", "map") }
        shouldFailCheck { map.checkHasValue(99, "map") }
        shouldFailCheck { map.checkContains("a", 99, "map") }
        shouldFailCheck { (null as Map<String, Int>?).checkHasKey("a", "map") }
    }
}
