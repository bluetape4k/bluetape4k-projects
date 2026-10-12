package io.bluetape4k.assertions.api

import io.bluetape4k.assertions.shouldBeFinite
import io.bluetape4k.assertions.shouldBeFiniteNegative
import io.bluetape4k.assertions.shouldBeFinitePositive
import io.bluetape4k.assertions.shouldBeFiniteZeroOrNegative
import io.bluetape4k.assertions.shouldBeFiniteZeroOrPositive
import io.bluetape4k.assertions.shouldBeNegative
import io.bluetape4k.assertions.shouldBePositive
import io.bluetape4k.assertions.shouldBeZeroOrPositive
import io.bluetape4k.assertions.shouldContainAny
import io.bluetape4k.assertions.shouldNot
import io.bluetape4k.assertions.shouldNotBeAfter
import io.bluetape4k.assertions.shouldNotBeBefore
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualToIgnoringCase
import io.bluetape4k.assertions.shouldNotBeFinite
import io.bluetape4k.assertions.shouldNotBeFiniteNegative
import io.bluetape4k.assertions.shouldNotBeFinitePositive
import io.bluetape4k.assertions.shouldNotBeFiniteZeroOrNegative
import io.bluetape4k.assertions.shouldNotBeFiniteZeroOrPositive
import io.bluetape4k.assertions.shouldNotBeNear
import io.bluetape4k.assertions.shouldNotBeNegative
import io.bluetape4k.assertions.shouldNotBeOnOrAfter
import io.bluetape4k.assertions.shouldNotBeOnOrBefore
import io.bluetape4k.assertions.shouldNotBePositive
import io.bluetape4k.assertions.shouldNotBeZero
import io.bluetape4k.assertions.shouldNotContainAll
import io.bluetape4k.assertions.shouldNotContainSame
import io.bluetape4k.assertions.shouldNotContentEqual
import io.bluetape4k.assertions.shouldNotHaveSize
import io.bluetape4k.assertions.shouldNotMatch
import io.bluetape4k.assertions.shouldNotMatchAllWith
import io.bluetape4k.assertions.shouldNotMatchAtLeastOneOf
import java.math.BigDecimal
import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.seconds

/** Assertions 공개 보완 API의 overload와 반환형을 컴파일 단계에서 고정한다. */
@Suppress("UNUSED_VARIABLE")
internal object AssertionsApiCompileFixture {

    fun compile() {
        compileBasicAndCharacterSequences()
        compileArrays()
        compileCollectionsAndMaps()
        compileDateTimes()
        compileNumbers()
        compileDurations()
    }

    private fun compileBasicAndCharacterSequences() {
        2.shouldNot("must be odd") { it % 2 == 1 }

        val nullableText: String? = "kotlin"
        val notMatch: String? = nullableText shouldNotMatch "\\d+"
        val notAll: String? = nullableText shouldNotContainAll listOf("java", "rust")
        val notAllVararg: String? = nullableText.shouldNotContainAll("java", "rust")
        val any: String = nullableText.shouldContainAny("kot", "java")
        val notCase: String? = nullableText shouldNotBeEqualToIgnoringCase "java"
    }

    private fun compileArrays() {
        val nullableIntArray: IntArray? = intArrayOf(1, 2)
        val nullableLongArray: LongArray? = longArrayOf(1, 2)
        val nullableDoubleArray: DoubleArray? = doubleArrayOf(1.0, 2.0)
        val nullableFloatArray: FloatArray? = floatArrayOf(1.0f, 2.0f)
        val nullableByteArray: ByteArray? = byteArrayOf(1, 2)
        val nullableShortArray: ShortArray? = shortArrayOf(1, 2)
        val nullableCharArray: CharArray? = charArrayOf('a', 'b')
        val nullableBooleanArray: BooleanArray? = booleanArrayOf(true, false)
        val nullableObjectArray: Array<String>? = arrayOf("a", "b")

        val intSize: IntArray? = nullableIntArray shouldNotHaveSize 3
        val longSize: LongArray? = nullableLongArray shouldNotHaveSize 3
        val doubleSize: DoubleArray? = nullableDoubleArray shouldNotHaveSize 3
        val floatSize: FloatArray? = nullableFloatArray shouldNotHaveSize 3
        val byteSize: ByteArray? = nullableByteArray shouldNotHaveSize 3
        val shortSize: ShortArray? = nullableShortArray shouldNotHaveSize 3
        val charSize: CharArray? = nullableCharArray shouldNotHaveSize 3
        val booleanSize: BooleanArray? = nullableBooleanArray shouldNotHaveSize 3
        val objectSize: Array<String>? = nullableObjectArray shouldNotHaveSize 3

        val intContent: IntArray? = nullableIntArray shouldNotContentEqual intArrayOf(2, 1)
        val longContent: LongArray? = nullableLongArray shouldNotContentEqual longArrayOf(2, 1)
        val doubleContent: DoubleArray? = nullableDoubleArray shouldNotContentEqual doubleArrayOf(2.0, 1.0)
        val floatContent: FloatArray? = nullableFloatArray shouldNotContentEqual floatArrayOf(2.0f, 1.0f)
        val byteContent: ByteArray? = nullableByteArray shouldNotContentEqual byteArrayOf(2, 1)
        val shortContent: ShortArray? = nullableShortArray shouldNotContentEqual shortArrayOf(2, 1)
        val charContent: CharArray? = nullableCharArray shouldNotContentEqual charArrayOf('b', 'a')
        val booleanContent: BooleanArray? = nullableBooleanArray shouldNotContentEqual booleanArrayOf(false, true)
        val objectContent: Array<String>? = nullableObjectArray shouldNotContentEqual arrayOf("b", "a")

        val objectNotEqual: Array<out String>? = nullableObjectArray shouldNotBeEqualTo arrayOf("c")
        val intNotEqual: IntArray? = nullableIntArray shouldNotBeEqualTo intArrayOf(2, 1)
        val longNotEqual: LongArray? = nullableLongArray shouldNotBeEqualTo longArrayOf(2, 1)
        val doubleNotEqual: DoubleArray? = nullableDoubleArray shouldNotBeEqualTo doubleArrayOf(2.0, 1.0)
        val floatNotEqual: FloatArray? = nullableFloatArray shouldNotBeEqualTo floatArrayOf(2.0f, 1.0f)
        val byteNotEqual: ByteArray? = nullableByteArray shouldNotBeEqualTo byteArrayOf(2, 1)
        val charNotEqual: CharArray? = nullableCharArray shouldNotBeEqualTo charArrayOf('b', 'a')
        val shortNotEqual: ShortArray? = nullableShortArray shouldNotBeEqualTo shortArrayOf(2, 1)
        val booleanNotEqual: BooleanArray? = nullableBooleanArray shouldNotBeEqualTo booleanArrayOf(false, true)

        val objectNotSame: Array<String>? = nullableObjectArray shouldNotContainSame arrayOf("a", "a")
        val intNotSame: IntArray? = nullableIntArray shouldNotContainSame intArrayOf(1, 1)
        val longNotSame: LongArray? = nullableLongArray shouldNotContainSame listOf(1L, 1L)
        val longArrayNotSame: LongArray? = nullableLongArray shouldNotContainSame longArrayOf(1, 1)
        val byteNotSame: ByteArray? = nullableByteArray shouldNotContainSame byteArrayOf(1, 1)
        val floatNotSame: FloatArray? = nullableFloatArray shouldNotContainSame floatArrayOf(1.0f, 1.0f)
        val doubleNotSame: DoubleArray? = nullableDoubleArray shouldNotContainSame doubleArrayOf(1.0, 1.0)
        val shortNotSame: ShortArray? = nullableShortArray shouldNotContainSame shortArrayOf(1, 1)
        val charNotSame: CharArray? = nullableCharArray shouldNotContainSame charArrayOf('a', 'a')
    }

    private fun compileCollectionsAndMaps() {
        val nullableItems: Iterable<Int>? = listOf(1, 2, 3)
        val notAllItems: Iterable<Int>? = nullableItems shouldNotContainAll listOf(1, 4)
        val notAllItemsVararg: Iterable<Int>? = nullableItems.shouldNotContainAll(1, 4)
        val notAllMatched: Iterable<Int>? = nullableItems shouldNotMatchAllWith { it > 2 }
        val notAnyMatched: Iterable<Int>? = nullableItems shouldNotMatchAtLeastOneOf { it > 3 }
        val notSize: Iterable<Int>? = nullableItems shouldNotHaveSize 4
        val notContent: Iterable<Int>? = nullableItems shouldNotContentEqual listOf(3, 2, 1)
        val notSame: Iterable<Int>? = nullableItems shouldNotContainSame listOf(1, 1, 2)
        val nullableSequence: Sequence<Int>? = sequenceOf(1, 2)
        val notSequenceContent: Sequence<Int>? = nullableSequence shouldNotContentEqual sequenceOf(2, 1)

        val nullableMap: Map<String, Int>? = mapOf("a" to 1)
        val notMapSize: Map<String, Int>? = nullableMap shouldNotHaveSize 2
        val notMapSame: Map<String, Int>? = nullableMap shouldNotContainSame mapOf("a" to 2)
    }

    private fun compileDateTimes() {
        val instant = Instant.parse("2026-05-07T10:00:00Z")
        instant shouldNotBeAfter instant.plusSeconds(1)
        instant shouldNotBeBefore instant.minusSeconds(1)
        instant shouldNotBeOnOrAfter instant.plusSeconds(1)
        instant shouldNotBeOnOrBefore instant.minusSeconds(1)
        val zoned = ZonedDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        zoned shouldNotBeAfter zoned.plusHours(1)
        zoned shouldNotBeBefore zoned.minusHours(1)
        zoned shouldNotBeOnOrAfter zoned.plusHours(1)
        zoned shouldNotBeOnOrBefore zoned.minusHours(1)
        val offset = OffsetDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        offset shouldNotBeAfter offset.plusHours(1)
        offset shouldNotBeBefore offset.minusHours(1)
        offset shouldNotBeOnOrAfter offset.plusHours(1)
        offset shouldNotBeOnOrBefore offset.minusHours(1)
        val localDateTime = LocalDateTime.of(2026, 5, 7, 10, 0)
        localDateTime shouldNotBeAfter localDateTime.plusHours(1)
        localDateTime shouldNotBeBefore localDateTime.minusHours(1)
        localDateTime shouldNotBeOnOrAfter localDateTime.plusHours(1)
        localDateTime shouldNotBeOnOrBefore localDateTime.minusHours(1)
        val localDate = LocalDate.of(2026, 5, 7)
        localDate shouldNotBeAfter localDate.plusDays(1)
        localDate shouldNotBeBefore localDate.minusDays(1)
        localDate shouldNotBeOnOrAfter localDate.plusDays(1)
        localDate shouldNotBeOnOrBefore localDate.minusDays(1)
        val localTime = LocalTime.of(10, 0)
        localTime shouldNotBeAfter localTime.plusHours(1)
        localTime shouldNotBeBefore localTime.minusHours(1)
        localTime shouldNotBeOnOrAfter localTime.plusHours(1)
        localTime shouldNotBeOnOrBefore localTime.minusHours(1)
        val date = Date(1_000)
        date shouldNotBeAfter Date(1_001)
        date shouldNotBeBefore Date(999)
        date shouldNotBeOnOrAfter Date(1_001)
        date shouldNotBeOnOrBefore Date(999)
    }

    private fun compileNumbers() {
        1.toByte().shouldNotBePositive()
        1.toByte().shouldNotBeNegative()
        1.toByte().shouldNotBeZero()
        1.toShort().shouldNotBePositive()
        1.toShort().shouldNotBeNegative()
        1.toShort().shouldNotBeZero()
        1.shouldNotBePositive()
        1.shouldNotBeNegative()
        1.shouldNotBeZero()
        1L.shouldNotBePositive()
        1L.shouldNotBeNegative()
        1L.shouldNotBeZero()
        1.0f.shouldNotBePositive()
        1.0f.shouldNotBeNegative()
        1.0f.shouldNotBeZero()
        1.0.shouldNotBePositive()
        1.0.shouldNotBeNegative()
        1.0.shouldNotBeZero()

        1.0f.shouldBeFinite()
        Float.NaN.shouldNotBeFinite()
        1.0.shouldBeFinite()
        Double.NaN.shouldNotBeFinite()
        1.0f.shouldBeFinitePositive()
        0.0f.shouldNotBeFinitePositive()
        0.0f.shouldBeFiniteZeroOrNegative()
        1.0f.shouldNotBeFiniteZeroOrNegative()
        0.0f.shouldBeFiniteZeroOrPositive()
        (-1.0f).shouldNotBeFiniteZeroOrPositive()
        (-1.0f).shouldBeFiniteNegative()
        0.0f.shouldNotBeFiniteNegative()
        1.0.shouldBeFinitePositive()
        0.0.shouldNotBeFinitePositive()
        0.0.shouldBeFiniteZeroOrNegative()
        1.0.shouldNotBeFiniteZeroOrNegative()
        0.0.shouldBeFiniteZeroOrPositive()
        (-1.0).shouldNotBeFiniteZeroOrPositive()
        (-1.0).shouldBeFiniteNegative()
        0.0.shouldNotBeFiniteNegative()

        1.0.shouldNotBeNear(2.0, 0.1)
        1.0 shouldNotBeNear 2.0
        1.0f.shouldNotBeNear(2.0f, 0.1f)
        1.0f shouldNotBeNear 2.0f
        BigDecimal("1.0").shouldNotBeNear(BigDecimal("2.0"), BigDecimal("0.1"))
        BigDecimal("1.0").shouldNotBeNear(BigDecimal("2.0"))
    }

    private fun compileDurations() {
        val javaDuration = JavaDuration.ofSeconds(1)
        val javaPositive: JavaDuration = javaDuration.shouldBePositive()
        val javaZeroOrPositive: JavaDuration = javaDuration.shouldBeZeroOrPositive()
        val javaNegative: JavaDuration = javaDuration.shouldBeNegative()
        val javaNotPositive: JavaDuration = javaDuration.shouldNotBePositive()
        val kotlinDuration: KotlinDuration = 1.seconds
        val kotlinPositive: KotlinDuration = kotlinDuration.shouldBePositive()
        val kotlinZeroOrPositive: KotlinDuration = kotlinDuration.shouldBeZeroOrPositive()
        val kotlinNegative: KotlinDuration = kotlinDuration.shouldBeNegative()
        val kotlinNotPositive: KotlinDuration = kotlinDuration.shouldNotBePositive()
    }
}
