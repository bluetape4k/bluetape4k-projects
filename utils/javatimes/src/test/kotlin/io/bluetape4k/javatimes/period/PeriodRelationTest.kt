package io.bluetape4k.javatimes.period

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class PeriodRelationTest {

    companion object : KLogging()

    @Test
    fun `all enum values are accessible`() {
        val values = PeriodRelation.entries
        values.isNotEmpty().shouldBeTrue()
        values.size shouldBeEqualTo 14
    }

    @Test
    fun `parse returns correct enum for exact name`() {
        PeriodRelation.parse("After") shouldBeEqualTo PeriodRelation.After
        PeriodRelation.parse("Before") shouldBeEqualTo PeriodRelation.Before
        PeriodRelation.parse("ExactMatch") shouldBeEqualTo PeriodRelation.ExactMatch
        PeriodRelation.parse("Inside") shouldBeEqualTo PeriodRelation.Inside
        PeriodRelation.parse("Enclosing") shouldBeEqualTo PeriodRelation.Enclosing
    }

    @Test
    fun `parse is case insensitive`() {
        PeriodRelation.parse("after").shouldNotBeNull() shouldBeEqualTo PeriodRelation.After
        PeriodRelation.parse("BEFORE").shouldNotBeNull() shouldBeEqualTo PeriodRelation.Before
        PeriodRelation.parse("exactmatch").shouldNotBeNull() shouldBeEqualTo PeriodRelation.ExactMatch
    }

    @Test
    fun `parse trims whitespace`() {
        PeriodRelation.parse("  After  ").shouldNotBeNull() shouldBeEqualTo PeriodRelation.After
    }

    @Test
    fun `parse returns null for unknown value`() {
        PeriodRelation.parse("Unknown").shouldBeNull()
        PeriodRelation.parse("").shouldBeNull()
    }

    @Test
    fun `NotOverlappedRelations contains expected values`() {
        val notOverlapped = PeriodRelation.NotOverlappedRelations
        notOverlapped.contains(PeriodRelation.NoRelation).shouldBeTrue()
        notOverlapped.contains(PeriodRelation.After).shouldBeTrue()
        notOverlapped.contains(PeriodRelation.StartTouching).shouldBeTrue()
        notOverlapped.contains(PeriodRelation.EndTouching).shouldBeTrue()
        notOverlapped.contains(PeriodRelation.Before).shouldBeTrue()
    }

    @Test
    fun `overlapping relations are not in NotOverlappedRelations`() {
        val notOverlapped = PeriodRelation.NotOverlappedRelations
        notOverlapped.contains(PeriodRelation.Inside).let { it.not().shouldBeTrue() }
        notOverlapped.contains(PeriodRelation.Enclosing).let { it.not().shouldBeTrue() }
        notOverlapped.contains(PeriodRelation.ExactMatch).let { it.not().shouldBeTrue() }
    }

    @Test
    fun `parse NoRelation`() {
        PeriodRelation.parse("NoRelation") shouldBeEqualTo PeriodRelation.NoRelation
    }

    @Test
    fun `parse StartTouching`() {
        PeriodRelation.parse("StartTouching") shouldBeEqualTo PeriodRelation.StartTouching
    }

    @Test
    fun `parse EndTouching`() {
        PeriodRelation.parse("EndTouching") shouldBeEqualTo PeriodRelation.EndTouching
    }

    @Test
    fun `parse InsideStartTouching`() {
        PeriodRelation.parse("InsideStartTouching") shouldBeEqualTo PeriodRelation.InsideStartTouching
    }

    @Test
    fun `parse EnclosingStartTouching`() {
        PeriodRelation.parse("EnclosingStartTouching") shouldBeEqualTo PeriodRelation.EnclosingStartTouching
    }

    @Test
    fun `parse EnclosingEndTouching`() {
        PeriodRelation.parse("EnclosingEndTouching") shouldBeEqualTo PeriodRelation.EnclosingEndTouching
    }

    @Test
    fun `parse InsideEndTouching`() {
        PeriodRelation.parse("InsideEndTouching") shouldBeEqualTo PeriodRelation.InsideEndTouching
    }

    @Test
    fun `parse EndInside`() {
        PeriodRelation.parse("EndInside") shouldBeEqualTo PeriodRelation.EndInside
    }

    @Test
    fun `parse StartInside`() {
        PeriodRelation.parse("StartInside") shouldBeEqualTo PeriodRelation.StartInside
    }
}
