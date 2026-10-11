package io.bluetape4k.assertions

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class AssertionSymmetryTest {

    @Test
    fun `custom predicate has an exact negative counterpart`() {
        var calls = 0
        2.shouldNot("must not be odd") {
            calls++
            it % 2 == 1
        }
        calls shouldBeEqualTo 1

        assertFailsWith<AssertionFailedError> {
            2.shouldNot("must not be even") { it % 2 == 0 }
        }
    }

    @Test
    fun `char sequence negative overloads complement positive overloads`() {
        "kotlin".shouldContainAny("java", "kot")
        "kotlin" shouldNotContainAll listOf("java", "rust")
        "kotlin".shouldNotContainAll("java", "rust")
        "kotlin" shouldNotMatch "\\d+"
        "Kotlin" shouldNotBeEqualToIgnoringCase "java"

        assertFailsWith<AssertionFailedError> { "kotlin".shouldContainAny("java", "rust") }
        assertFailsWith<AssertionFailedError> { "kotlin" shouldNotContainAll listOf("kot") }
        assertFailsWith<AssertionFailedError> { "kotlin".shouldNotContainAll(emptyList()) }
        assertFailsWith<AssertionFailedError> { "kotlin".shouldNotContainAll() }
        assertFailsWith<AssertionFailedError> { "kotlin" shouldNotMatch "kotlin" }
        assertFailsWith<AssertionFailedError> { "Kotlin" shouldNotBeEqualToIgnoringCase "kotlin" }

        val nullText: String? = null
        nullText shouldNotContainAll emptyList()
        nullText.shouldNotContainAll()
    }

    @Test
    fun `array size content and same-content negatives preserve null semantics`() {
        intArrayOf(1, 2).shouldNotHaveSize(3)
        intArrayOf(1, 2).shouldNotContentEqual(intArrayOf(2, 1))
        intArrayOf(1, 2).shouldNotContainSame(intArrayOf(1, 1))
        intArrayOf(1, 2).shouldNotBeEqualTo(intArrayOf(2, 1))
        arrayOf("a", "b").shouldNotHaveSize(1)
        arrayOf("a", "b").shouldNotContentEqual(arrayOf("b", "a"))
        arrayOf("a", "b").shouldNotContainSame(arrayOf("a", "a"))
        arrayOf("a", "b").shouldNotBeEqualTo(arrayOf("b", "a"))

        val nullable: IntArray? = null
        nullable.shouldNotHaveSize(1)
        nullable.shouldNotContentEqual(intArrayOf(1))
        nullable.shouldNotContainSame(intArrayOf(1))
        assertFailsWith<AssertionFailedError> { nullable.shouldNotBeEqualTo(intArrayOf(1)) }
    }

    @Test
    fun `collection negative overloads complement positive predicates`() {
        listOf(1, 2, 3) shouldNotContainAll listOf(1, 4)
        listOf(1, 2, 3) shouldNotMatchAllWith { it > 2 }
        listOf(1, 2, 3) shouldNotMatchAtLeastOneOf { it > 3 }
        listOf(1, 2, 3) shouldNotHaveSize(4)
        listOf(1, 2, 3) shouldNotContentEqual listOf(3, 2, 1)
        listOf(1, 2, 2) shouldNotContainSame listOf(1, 2, 3)
        sequenceOf(1, 2) shouldNotContentEqual sequenceOf(2, 1)

        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotContainAll listOf(1, 2) }
        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotMatchAllWith { it > 0 } }
        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotMatchAtLeastOneOf { it > 1 } }
        assertFailsWith<AssertionFailedError> { emptyList<Int>() shouldNotMatchAllWith { true } }
        val nullable: List<Int>? = null
        assertFailsWith<AssertionFailedError> { nullable shouldNotMatchAllWith { true } }
        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotHaveSize(2) }
        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotContentEqual listOf(1, 2) }
        assertFailsWith<AssertionFailedError> { listOf(1, 2) shouldNotContainSame listOf(2, 1) }
        assertFailsWith<AssertionFailedError> { sequenceOf(1, 2) shouldNotContentEqual sequenceOf(1, 2) }
    }

    @Test
    fun `map pair and size negatives distinguish missing null entry`() {
        val map = mapOf("present" to null)
        map shouldContain ("present" to null)
        map shouldNotContain ("missing" to null)
        map shouldNotHaveSize(2)
        map shouldNotContainSame mapOf("missing" to null)

        assertFailsWith<AssertionFailedError> { map shouldContain ("missing" to null) }
        assertFailsWith<AssertionFailedError> { map shouldNotContain ("present" to null) }
        assertFailsWith<AssertionFailedError> { map shouldNotHaveSize(1) }
        assertFailsWith<AssertionFailedError> { map shouldNotContainSame map }

        val nullableMap: Map<String, Int>? = null
        nullableMap shouldNotHaveSize 0
    }

    @Test
    fun `date time comparisons have negative counterparts`() {
        val instant = Instant.parse("2026-05-07T10:00:00Z")
        instant shouldNotBeAfter instant
        instant shouldNotBeBefore instant
        assertFailsWith<AssertionFailedError> { instant shouldNotBeOnOrAfter instant }
        assertFailsWith<AssertionFailedError> { instant shouldNotBeOnOrBefore instant }
        instant.minusSeconds(1) shouldNotBeAfter instant
        instant.plusSeconds(1) shouldNotBeBefore instant
        instant.minusSeconds(1) shouldNotBeOnOrAfter instant
        instant.plusSeconds(1) shouldNotBeOnOrBefore instant

        val zoned = ZonedDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        zoned shouldNotBeAfter zoned
        zoned shouldNotBeBefore zoned
        assertFailsWith<AssertionFailedError> { zoned shouldNotBeOnOrAfter zoned }
        assertFailsWith<AssertionFailedError> { zoned shouldNotBeOnOrBefore zoned }
        zoned.minusHours(1) shouldNotBeAfter zoned
        zoned.plusHours(1) shouldNotBeBefore zoned
        zoned.minusHours(1) shouldNotBeOnOrAfter zoned
        zoned.plusHours(1) shouldNotBeOnOrBefore zoned

        val offset = OffsetDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        offset shouldNotBeAfter offset
        offset shouldNotBeBefore offset
        assertFailsWith<AssertionFailedError> { offset shouldNotBeOnOrAfter offset }
        assertFailsWith<AssertionFailedError> { offset shouldNotBeOnOrBefore offset }
        offset.minusHours(1) shouldNotBeAfter offset
        offset.plusHours(1) shouldNotBeBefore offset
        offset.minusHours(1) shouldNotBeOnOrAfter offset
        offset.plusHours(1) shouldNotBeOnOrBefore offset

        val localDateTime = LocalDateTime.of(2026, 5, 7, 10, 0)
        localDateTime shouldNotBeAfter localDateTime
        localDateTime shouldNotBeBefore localDateTime
        assertFailsWith<AssertionFailedError> { localDateTime shouldNotBeOnOrAfter localDateTime }
        assertFailsWith<AssertionFailedError> { localDateTime shouldNotBeOnOrBefore localDateTime }
        localDateTime.minusHours(1) shouldNotBeAfter localDateTime
        localDateTime.plusHours(1) shouldNotBeBefore localDateTime
        localDateTime.minusHours(1) shouldNotBeOnOrAfter localDateTime
        localDateTime.plusHours(1) shouldNotBeOnOrBefore localDateTime

        val date = Date(1_000)
        date shouldNotBeAfter date
        date shouldNotBeBefore date
        assertFailsWith<AssertionFailedError> { date shouldNotBeOnOrAfter date }
        assertFailsWith<AssertionFailedError> { date shouldNotBeOnOrBefore date }
        Date(999) shouldNotBeAfter date
        Date(1_001) shouldNotBeBefore date
        Date(999) shouldNotBeOnOrAfter date
        Date(1_001) shouldNotBeOnOrBefore date

        val localDate = LocalDate.of(2026, 5, 7)
        localDate shouldNotBeAfter localDate
        localDate shouldNotBeBefore localDate
        assertFailsWith<AssertionFailedError> { localDate shouldNotBeOnOrAfter localDate }
        assertFailsWith<AssertionFailedError> { localDate shouldNotBeOnOrBefore localDate }
        localDate.minusDays(1) shouldNotBeAfter localDate
        localDate.plusDays(1) shouldNotBeBefore localDate
        localDate.minusDays(1) shouldNotBeOnOrAfter localDate
        localDate.plusDays(1) shouldNotBeOnOrBefore localDate

        val localTime = LocalTime.of(10, 0)
        localTime shouldNotBeAfter localTime
        localTime shouldNotBeBefore localTime
        assertFailsWith<AssertionFailedError> { localTime shouldNotBeOnOrAfter localTime }
        assertFailsWith<AssertionFailedError> { localTime shouldNotBeOnOrBefore localTime }
        localTime.minusHours(1) shouldNotBeAfter localTime
        localTime.plusHours(1) shouldNotBeBefore localTime
        localTime.minusHours(1) shouldNotBeOnOrAfter localTime
        localTime.plusHours(1) shouldNotBeOnOrBefore localTime
    }

    @Test
    fun `numeric signs are paired and NaN follows strict negation`() {
        1.toByte().shouldNotBeNegative()
        (-1).toByte().shouldNotBePositive()
        0.toShort().shouldNotBePositive()
        0.shouldNotBeNegative()
        0L.shouldNotBePositive()
        1.0f.shouldNotBeNegative()
        (-1.0).shouldNotBePositive()
        0.0.shouldNotBeNegative()

        Double.NaN.shouldNotBePositive()
        Double.NaN.shouldNotBeNegative()
        Double.NaN.shouldNotBeZero()
        Float.NaN.shouldNotBePositive()
        Float.NaN.shouldNotBeNegative()
        Float.NaN.shouldNotBeZero()
        Double.POSITIVE_INFINITY.shouldNotBeNegative()
        Double.POSITIVE_INFINITY.shouldNotBeZero()
        Double.NEGATIVE_INFINITY.shouldNotBePositive()
        Double.NEGATIVE_INFINITY.shouldNotBeZero()
        Float.POSITIVE_INFINITY.shouldNotBeNegative()
        Float.POSITIVE_INFINITY.shouldNotBeZero()
        Float.NEGATIVE_INFINITY.shouldNotBePositive()
        Float.NEGATIVE_INFINITY.shouldNotBeZero()
        (-0.0).shouldNotBePositive()
        (-0.0).shouldNotBeNegative()
        (-0.0f).shouldNotBePositive()
        (-0.0f).shouldNotBeNegative()

        assertFailsWith<AssertionFailedError> { 1.shouldNotBePositive() }
        assertFailsWith<AssertionFailedError> { (-1).shouldNotBeNegative() }
        assertFailsWith<AssertionFailedError> { 0.shouldNotBeZero() }
    }

    @Test
    fun `finite and finite-sign comparisons reject non-finite operands`() {
        1.0.shouldBeFinite()
        Double.NaN.shouldNotBeFinite()
        Double.POSITIVE_INFINITY.shouldNotBeFinite()
        Double.NEGATIVE_INFINITY.shouldNotBeFinite()
        0.0.shouldNotBeFinitePositive()
        0.0.shouldBeFiniteZeroOrPositive()
        (-0.0).shouldBeFiniteZeroOrNegative()
        (-1.0).shouldBeFiniteNegative()

        1.0f.shouldBeFinite()
        Float.NaN.shouldNotBeFinite()
        Float.POSITIVE_INFINITY.shouldNotBeFinite()
        Float.NEGATIVE_INFINITY.shouldNotBeFinite()
        1.0f.shouldBeFinitePositive()
        0.0f.shouldNotBeFinitePositive()
        0.0f.shouldBeFiniteZeroOrNegative()
        1.0f.shouldNotBeFiniteZeroOrNegative()
        0.0f.shouldBeFiniteZeroOrPositive()
        (-1.0f).shouldNotBeFiniteZeroOrPositive()
        (-1.0f).shouldBeFiniteNegative()
        0.0f.shouldNotBeFiniteNegative()

        assertFailsWith<AssertionFailedError> { Double.NaN.shouldBeFinite() }
        assertFailsWith<AssertionFailedError> { Double.POSITIVE_INFINITY.shouldBeFinitePositive() }
        assertFailsWith<AssertionFailedError> { Double.NEGATIVE_INFINITY.shouldBeFiniteZeroOrNegative() }
        assertFailsWith<AssertionFailedError> { 1.0.shouldBeFiniteZeroOrNegative() }
        assertFailsWith<AssertionFailedError> { (-1.0).shouldBeFiniteZeroOrPositive() }
        assertFailsWith<AssertionFailedError> { Double.NaN.shouldNotBeFinitePositive() }
        assertFailsWith<AssertionFailedError> { Float.POSITIVE_INFINITY.shouldNotBeFiniteZeroOrNegative() }

        1.0.shouldBeNear(1.125, tolerance = 0.125)
        assertFailsWith<AssertionFailedError> { 1.0.shouldNotBeNear(1.125, delta = 0.125) }
        1.0.shouldNotBeNear(1.2, delta = 0.1)
        1.0f.shouldBeNear(1.5f, tolerance = 0.5f)
        assertFailsWith<AssertionFailedError> { 1.0f.shouldNotBeNear(1.5f, delta = 0.5f) }
        BigDecimal("1.0").shouldNotBeNear(BigDecimal("1.2"), BigDecimal("0.1"))
        BigDecimal("1.0").shouldBeNear(BigDecimal("1.1"), BigDecimal("0.1"))
        assertFailsWith<AssertionFailedError> {
            BigDecimal("1.0").shouldNotBeNear(BigDecimal("1.1"), BigDecimal("0.1"))
        }
        assertFailsWith<IllegalArgumentException> { 1.0.shouldNotBeNear(1.0, Double.NEGATIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { 1.0f.shouldNotBeNear(1.0f, Float.NEGATIVE_INFINITY) }
        1.0.shouldBeNear(1.0, Double.POSITIVE_INFINITY)
        assertFailsWith<AssertionFailedError> { 1.0.shouldNotBeNear(1.0, Double.POSITIVE_INFINITY) }
        1.0f.shouldBeNear(1.0f, Float.POSITIVE_INFINITY)
        assertFailsWith<AssertionFailedError> { 1.0f.shouldNotBeNear(1.0f, Float.POSITIVE_INFINITY) }
        Double.POSITIVE_INFINITY.shouldBeNear(Double.POSITIVE_INFINITY)
        assertFailsWith<AssertionFailedError> {
            Double.POSITIVE_INFINITY.shouldNotBeNear(Double.POSITIVE_INFINITY)
        }
        Float.NEGATIVE_INFINITY.shouldBeNear(Float.NEGATIVE_INFINITY)
        assertFailsWith<AssertionFailedError> {
            Float.NEGATIVE_INFINITY.shouldNotBeNear(Float.NEGATIVE_INFINITY)
        }
    }
}
