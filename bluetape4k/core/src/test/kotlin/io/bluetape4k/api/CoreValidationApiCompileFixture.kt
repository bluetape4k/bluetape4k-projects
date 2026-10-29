package io.bluetape4k.api

import io.bluetape4k.javatimes.checkNegativeDuration as checkJavaNegativeDuration
import io.bluetape4k.javatimes.checkPositiveDuration as checkJavaPositiveDuration
import io.bluetape4k.javatimes.checkZeroOrNegativeDuration as checkJavaZeroOrNegativeDuration
import io.bluetape4k.javatimes.checkZeroOrPositiveDuration as checkJavaZeroOrPositiveDuration
import io.bluetape4k.javatimes.requireNegativeDuration as requireJavaNegativeDuration
import io.bluetape4k.javatimes.requirePositiveDuration as requireJavaPositiveDuration
import io.bluetape4k.javatimes.requireZeroOrNegativeDuration as requireJavaZeroOrNegativeDuration
import io.bluetape4k.javatimes.requireZeroOrPositiveDuration as requireJavaZeroOrPositiveDuration
import io.bluetape4k.support.checkContains
import io.bluetape4k.support.checkEndsWith
import io.bluetape4k.support.checkEquals
import io.bluetape4k.support.checkFinite
import io.bluetape4k.support.checkFiniteNegativeNumber
import io.bluetape4k.support.checkFinitePositiveNumber
import io.bluetape4k.support.checkFiniteZeroOrNegativeNumber
import io.bluetape4k.support.checkFiniteZeroOrPositiveNumber
import io.bluetape4k.support.checkHasKey
import io.bluetape4k.support.checkHasValue
import io.bluetape4k.support.checkInOpenRange
import io.bluetape4k.support.checkInRange
import io.bluetape4k.support.checkLengthInRange
import io.bluetape4k.support.checkNegativeNumber
import io.bluetape4k.support.checkNotBlank
import io.bluetape4k.support.checkNotContains
import io.bluetape4k.support.checkNotEmpty
import io.bluetape4k.support.checkNotEndsWith
import io.bluetape4k.support.checkNotEquals
import io.bluetape4k.support.checkNotFinite
import io.bluetape4k.support.checkNotHasKey
import io.bluetape4k.support.checkNotHasValue
import io.bluetape4k.support.checkNotInOpenRange
import io.bluetape4k.support.checkNotInRange
import io.bluetape4k.support.checkNotLengthInRange
import io.bluetape4k.support.checkNotSizeInRange
import io.bluetape4k.support.checkNotStartsWith
import io.bluetape4k.support.checkNullOrBlank
import io.bluetape4k.support.checkNullOrEmpty
import io.bluetape4k.support.checkPositiveNumber
import io.bluetape4k.support.checkPositiveDuration as checkKotlinPositiveDuration
import io.bluetape4k.support.checkNegativeDuration as checkKotlinNegativeDuration
import io.bluetape4k.support.checkZeroOrNegativeDuration as checkKotlinZeroOrNegativeDuration
import io.bluetape4k.support.checkZeroOrPositiveDuration as checkKotlinZeroOrPositiveDuration
import io.bluetape4k.support.checkSizeInRange
import io.bluetape4k.support.checkStartsWith
import io.bluetape4k.support.checkZeroOrNegativeNumber
import io.bluetape4k.support.checkZeroOrPositiveNumber
import io.bluetape4k.support.requireContains
import io.bluetape4k.support.requireEndsWith
import io.bluetape4k.support.requireEquals
import io.bluetape4k.support.requireFinite
import io.bluetape4k.support.requireFiniteNegativeNumber
import io.bluetape4k.support.requireFinitePositiveNumber
import io.bluetape4k.support.requireFiniteZeroOrNegativeNumber
import io.bluetape4k.support.requireFiniteZeroOrPositiveNumber
import io.bluetape4k.support.requireHasKey
import io.bluetape4k.support.requireHasValue
import io.bluetape4k.support.requireInOpenRange
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireLengthInRange
import io.bluetape4k.support.requireNegativeNumber
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotContains
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requireNotEndsWith
import io.bluetape4k.support.requireNotEquals
import io.bluetape4k.support.requireNotFinite
import io.bluetape4k.support.requireNotHasKey
import io.bluetape4k.support.requireNotHasValue
import io.bluetape4k.support.requireNotInOpenRange
import io.bluetape4k.support.requireNotInRange
import io.bluetape4k.support.requireNotLengthInRange
import io.bluetape4k.support.requireNotSizeInRange
import io.bluetape4k.support.requireNotStartsWith
import io.bluetape4k.support.requireNegative
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.support.requireNullOrBlank
import io.bluetape4k.support.requireNullOrEmpty
import io.bluetape4k.support.requirePositiveDuration as requireKotlinPositiveDuration
import io.bluetape4k.support.requireNegativeDuration as requireKotlinNegativeDuration
import io.bluetape4k.support.requireZeroOrNegativeDuration as requireKotlinZeroOrNegativeDuration
import io.bluetape4k.support.requireZeroOrPositiveDuration as requireKotlinZeroOrPositiveDuration
import io.bluetape4k.support.requireSizeInRange
import io.bluetape4k.support.requireStartsWith
import io.bluetape4k.support.requireZeroOrNegativeNumber
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.time.Duration as JavaDuration
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.seconds

/** Core helper API compile-only coverage for the selected validation families. */
internal object CoreValidationApiCompileFixture {

    fun compile() {
        compileNullableCollections()
        compileStringAndComparableHelpers()
        compileRangeHelpers()
        compileMapHelpers()
        compileNumberHelpers()
        compileDurationHelpers()
    }

    private fun compileNullableCollections() {
        val nullableText: String? = "blue"
        val nullableArray: Array<Int>? = emptyArray()
        val nullableCollection: Collection<Int>? = emptyList()
        val nullableMap: Map<String, Int>? = emptyMap()

        val nullableEmptyText: String? = nullableText.requireNullOrEmpty("text")
        val nullableBlankText: String? = nullableText.requireNullOrBlank("text")
        val nullableEmptyArray: Array<Int>? = nullableArray.requireNullOrEmpty("items")
        val nullableEmptyCollection: Collection<Int>? = nullableCollection.requireNullOrEmpty("items")
        val nullableEmptyMap: Map<String, Int>? = nullableMap.requireNullOrEmpty("items")
        val nonEmptyText: String = nullableText.requireNotEmpty("text")
        val nonEmptyArray: Array<Int> = nullableArray.requireNotEmpty("items")
        val nonEmptyCollection: Collection<Int> = nullableCollection.requireNotEmpty("items")
        val nonEmptyMap: Map<String, Int> = nullableMap.requireNotEmpty("items")
        val checkedEmptyText: String? = nullableText.checkNullOrEmpty("text")
        val checkedBlankText: String? = nullableText.checkNullOrBlank("text")
        val checkedEmptyArray: Array<Int>? = nullableArray.checkNullOrEmpty("items")
        val checkedEmptyCollection: Collection<Int>? = nullableCollection.checkNullOrEmpty("items")
        val checkedEmptyMap: Map<String, Int>? = nullableMap.checkNullOrEmpty("items")
        val checkedText: String = nullableText.checkNotEmpty("text")
        val checkedArray: Array<Int> = nullableArray.checkNotEmpty("items")
        val checkedCollection: Collection<Int> = nullableCollection.checkNotEmpty("items")
        val checkedMap: Map<String, Int> = nullableMap.checkNotEmpty("items")

        listOf(
            nullableEmptyText,
            nullableBlankText,
            nullableEmptyArray,
            nullableEmptyCollection,
            nullableEmptyMap,
            nonEmptyText,
            nonEmptyArray,
            nonEmptyCollection,
            nonEmptyMap,
            checkedEmptyText,
            checkedBlankText,
            checkedEmptyArray,
            checkedEmptyCollection,
            checkedEmptyMap,
            checkedText,
            checkedArray,
            checkedCollection,
            checkedMap,
        )
    }

    private fun compileStringAndComparableHelpers() {
        val nullableText: String? = "blue"
        nullableText.requireContains("l", "text")
        nullableText.requireNotContains("z", "text")
        nullableText.requireStartsWith("b", "text")
        nullableText.requireNotStartsWith("z", "text")
        nullableText.requireEndsWith("e", "text")
        nullableText.requireNotEndsWith("z", "text")
        nullableText.requireEndsWith("e", false) { "ends" }
        nullableText.requireNotEndsWith("z", false) { "ends" }
        nullableText.checkContains("l", "text")
        nullableText.checkNotContains("z", "text")
        nullableText.checkStartsWith("b", "text")
        nullableText.checkNotStartsWith("z", "text")
        nullableText.checkEndsWith("e", "text")
        nullableText.checkNotEndsWith("z", "text")

        1.requireEquals(1, "value")
        1.requireNotEquals(2, "value")
        1.checkEquals(1, "value")
        1.checkNotEquals(2, "value")
        1.requireEquals(1) { "equals" }
        1.requireNotEquals(2) { "not equals" }
        1.checkEquals(1) { "equals" }
        1.checkNotEquals(2) { "not equals" }
    }

    private fun compileRangeHelpers() {
        val nullableText: String? = "blue"
        val nullableArray: Array<Int>? = emptyArray()
        val nullableCollection: Collection<Int>? = emptyList()
        val nullableMap: Map<String, Int>? = emptyMap()
        5.requireInRange(1, 10, "value")
        0.requireNotInRange(1, 10, "value")
        5.requireInOpenRange(1, 10, "value")
        10.requireNotInOpenRange(1, 10, "value")
        5.checkInRange(1, 10, "value")
        0.checkNotInRange(1, 10, "value")
        5.checkInOpenRange(1, 10, "value")
        10.checkNotInOpenRange(1, 10, "value")
        0.requireNotInRange(1, 10) { "range" }
        10.requireNotInOpenRange(1, 10) { "open range" }
        0.checkNotInRange(1, 10) { "range" }
        10.checkNotInOpenRange(1, 10) { "open range" }

        nullableText.requireLengthInRange(1, 10, "text")
        nullableText.requireNotLengthInRange(11, 20, "text")
        nullableCollection.requireSizeInRange(0, 10, "items")
        nullableCollection.requireNotSizeInRange(11, 20, "items")
        nullableMap.requireSizeInRange(0, 10, "items")
        nullableMap.requireNotSizeInRange(11, 20, "items")
        nullableArray.requireSizeInRange(0, 10, "items")
        nullableArray.requireNotSizeInRange(11, 20, "items")
        nullableText.checkLengthInRange(1, 10, "text")
        nullableText.checkNotLengthInRange(11, 20, "text")
        nullableCollection.checkSizeInRange(0, 10, "items")
        nullableCollection.checkNotSizeInRange(11, 20, "items")
        nullableMap.checkSizeInRange(0, 10, "items")
        nullableMap.checkNotSizeInRange(11, 20, "items")
        nullableArray.checkSizeInRange(0, 10, "items")
        nullableArray.checkNotSizeInRange(11, 20, "items")
    }

    private fun compileMapHelpers() {
        val map = mapOf("a" to 1)
        map.requireHasKey("a", "map")
        map.requireNotHasKey("b", "map")
        map.requireHasValue(1, "map")
        map.requireNotHasValue(2, "map")
        map.requireContains("a", 1, "map")
        map.requireNotContains("a", 2, "map")
        map.checkHasKey("a", "map")
        map.checkNotHasKey("b", "map")
        map.checkHasValue(1, "map")
        map.checkNotHasValue(2, "map")
        map.checkContains("a", 1, "map")
        map.checkNotContains("a", 2, "map")
    }

    private fun compileNumberHelpers() {
        1.0f.requireFinite("value")
        Float.NaN.requireNotFinite("value")
        1.0.checkFinite("value")
        Double.POSITIVE_INFINITY.checkNotFinite("value")
        1.0f.requirePositiveNumber("value")
        (-1.0f).requireZeroOrNegativeNumber("value")
        1.0.checkPositiveNumber { "positive" }
        (-1.0).checkNegativeNumber { "negative" }
        1.0f.requireFinitePositiveNumber("value")
        0.0f.requireFiniteZeroOrNegativeNumber("value")
        0.0.requireFiniteZeroOrPositiveNumber("value")
        (-1.0).requireFiniteNegativeNumber("value")
        1.0f.checkFinitePositiveNumber("value")
        0.0f.checkFiniteZeroOrNegativeNumber("value")
        0.0.checkFiniteZeroOrPositiveNumber("value")
        (-1.0).checkFiniteNegativeNumber("value")
    }

    private fun compileDurationHelpers() {
        val javaDuration = JavaDuration.ofSeconds(1)
        JavaDuration.ofSeconds(-1).requireNegative("duration")
        JavaDuration.ofSeconds(-1).requireNegative { "duration" }
        javaDuration.requireJavaPositiveDuration("duration")
        JavaDuration.ZERO.requireJavaZeroOrPositiveDuration("duration")
        JavaDuration.ofSeconds(-1).requireJavaNegativeDuration("duration")
        JavaDuration.ZERO.requireJavaZeroOrNegativeDuration("duration")
        javaDuration.checkJavaPositiveDuration("duration")
        JavaDuration.ZERO.checkJavaZeroOrPositiveDuration("duration")
        JavaDuration.ofSeconds(-1).checkJavaNegativeDuration("duration")
        JavaDuration.ZERO.checkJavaZeroOrNegativeDuration("duration")

        val kotlinDuration = 1.seconds
        (-1).seconds.requireNegative("duration")
        (-1).seconds.requireNegative { "duration" }
        kotlinDuration.requireKotlinPositiveDuration("duration")
        KotlinDuration.ZERO.requireKotlinZeroOrPositiveDuration("duration")
        (-1).seconds.requireKotlinNegativeDuration("duration")
        KotlinDuration.ZERO.requireKotlinZeroOrNegativeDuration("duration")
        kotlinDuration.checkKotlinPositiveDuration("duration")
        KotlinDuration.ZERO.checkKotlinZeroOrPositiveDuration("duration")
        (-1).seconds.checkKotlinNegativeDuration("duration")
        KotlinDuration.ZERO.checkKotlinZeroOrNegativeDuration("duration")
    }
}
