package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RangeBoundaryTest {

    companion object : KLogging() {

        data class RangeSpec(
            val label: String,
            val toRange: (ClosedRange<Int>) -> Range<Int>,
            val factory: (Int, Int) -> Range<Int>,
            val isStartInclusive: Boolean,
            val isEndInclusive: Boolean,
            val sameStartEndIsEmpty: Boolean,
            val containsStart: Boolean,
            val containsEnd: Boolean,
        ) {
            override fun toString() = label
        }

        @JvmStatic
        fun rangeSpecs(): List<RangeSpec> = listOf(
            RangeSpec(
                label = "ClosedClosed [start, end]",
                toRange = { it.toClosedClosedRange() },
                factory = { s, e -> closedClosedRangeOf(s, e) },
                isStartInclusive = true,
                isEndInclusive = true,
                sameStartEndIsEmpty = false,
                containsStart = true,
                containsEnd = true,
            ),
            RangeSpec(
                label = "ClosedOpen [start, end)",
                toRange = { it.toClosedOpenRange() },
                factory = { s, e -> closedOpenRangeOf(s, e) },
                isStartInclusive = true,
                isEndInclusive = false,
                sameStartEndIsEmpty = true,
                containsStart = true,
                containsEnd = false,
            ),
            RangeSpec(
                label = "OpenClosed (start, end]",
                toRange = { it.toOpenClosedRange() },
                factory = { s, e -> openClosedRangeOf(s, e) },
                isStartInclusive = false,
                isEndInclusive = true,
                sameStartEndIsEmpty = true,
                containsStart = false,
                containsEnd = true,
            ),
            RangeSpec(
                label = "OpenOpen (start, end)",
                toRange = { it.toOpenOpenRange() },
                factory = { s, e -> openOpenRangeOf(s, e) },
                isStartInclusive = false,
                isEndInclusive = false,
                sameStartEndIsEmpty = true,
                containsStart = false,
                containsEnd = false,
            ),
        )
    }

    @ParameterizedTest(name = "{0} :: create by factory function")
    @MethodSource("rangeSpecs")
    fun `create by factory function`(spec: RangeSpec) {
        val range = spec.factory(0, 10)
        range.first shouldBeEqualTo 0
        range.last shouldBeEqualTo 10
        range.isEmpty().shouldBeFalse()
    }

    @ParameterizedTest(name = "{0} :: ClosedRange -> Range conversion")
    @MethodSource("rangeSpecs")
    fun `ClosedRange to Range conversion`(spec: RangeSpec) {
        spec.toRange(0..10) shouldBeEqualTo spec.factory(0, 10)
    }

    @ParameterizedTest(name = "{0} :: boundary properties")
    @MethodSource("rangeSpecs")
    fun `boundary properties`(spec: RangeSpec) {
        val range = spec.factory(0, 10)
        range.isStartInclusive shouldBeEqualTo spec.isStartInclusive
        range.isEndInclusive shouldBeEqualTo spec.isEndInclusive
    }

    @ParameterizedTest(name = "{0} :: isEmpty for same start/end")
    @MethodSource("rangeSpecs")
    fun `isEmpty for same start end`(spec: RangeSpec) {
        spec.factory(1, 2).isEmpty().shouldBeFalse()
        spec.factory(2, 1).isEmpty().shouldBeTrue()
        spec.factory(1, 1).isEmpty() shouldBeEqualTo spec.sameStartEndIsEmpty
    }

    @ParameterizedTest(name = "{0} :: range contains element")
    @MethodSource("rangeSpecs")
    fun `range contains element`(spec: RangeSpec) {
        val range = spec.factory(0, 10)
        range.contains(3).shouldBeTrue()
        range.contains(9).shouldBeTrue()
        range.contains(0) shouldBeEqualTo spec.containsStart
        range.contains(10) shouldBeEqualTo spec.containsEnd
    }

    @ParameterizedTest(name = "{0} :: range contains range")
    @MethodSource("rangeSpecs")
    fun `range contains range`(spec: RangeSpec) {
        val larger = spec.factory(0, 10)
        val inner = spec.factory(4, 8)
        val outer = spec.factory(20, 30)
        val intersect = spec.factory(5, 15)

        larger.contains(larger).shouldBeTrue()
        larger.contains(inner).shouldBeTrue()
        larger.contains(outer).shouldBeFalse()
        larger.contains(intersect).shouldBeFalse()
    }

    @Test
    fun `build ClosedOpenRange by until operator`() {
        (1 until 3) shouldBeInstanceOf ClosedOpenRange::class
        (3 until 1) shouldBeInstanceOf ClosedOpenRange::class
    }
}
