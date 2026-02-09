package io.bluetape4k.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeSupportTest {

    companion object: KLogging()

    private fun longRanges(vararg ranges: IntRange): List<LongRange> =
        ranges.map { it.first.toLong()..it.last.toLong() }


    @Test
    fun `range is ascending`() {
        longRanges(0..10, 1..5, 12..13).isAscending().shouldBeTrue()
        longRanges(2..10, 1..5, 5..13).isAscending().shouldBeFalse()
    }

    @Test
    fun `Range interface isAscending`() {
        // OpenOpenRange는 Range<T>만 구현하므로 오버로드 충돌 없음
        val ascending = listOf(
            openOpenRangeOf(0, 10),
            openOpenRangeOf(1, 5),
            openOpenRangeOf(12, 13),
        )
        ascending.isAscending().shouldBeTrue()

        val notAscending = listOf(
            openOpenRangeOf(2, 10),
            openOpenRangeOf(1, 5),
            openOpenRangeOf(5, 13),
        )
        notAscending.isAscending().shouldBeFalse()

        // 단일 원소
        listOf(openOpenRangeOf(1, 5)).isAscending().shouldBeTrue()

        // 빈 컬렉션
        emptyList<Range<Int>>().isAscending().shouldBeTrue()
    }

    @Test
    fun `create closed range by BigDecimal`() {
        val range = 5.toBigDecimal()..10.toBigDecimal()

        assertTrue { 6.toBigDecimal() in range }
        assertFalse { 3.toBigDecimal() in range }

        range.contains(7.toBigDecimal()..10.toBigDecimal()).shouldBeTrue()
        range.contains(7.toBigDecimal()..11.toBigDecimal()).shouldBeFalse()
        range.contains(4.toBigDecimal()..10.toBigDecimal()).shouldBeFalse()
    }

    @Test
    fun `create closed range by BigInteger`() {
        val range = 5.toBigInteger()..10.toBigInteger()

        assertTrue { 6.toBigInteger() in range }
        assertFalse { 3.toBigInteger() in range }

        range.contains(7.toBigInteger()..10.toBigInteger()).shouldBeTrue()
        range.contains(7.toBigInteger()..11.toBigInteger()).shouldBeFalse()
        range.contains(4.toBigInteger()..10.toBigInteger()).shouldBeFalse()
    }

    @Test
    fun `cross-type range contains with boundary awareness`() {
        // [0, 10] contains (2, 8) -> true
        val closed = closedClosedRangeOf(0, 10)
        val openInner = openOpenRangeOf(2, 8)
        closed.contains(openInner).shouldBeTrue()

        // (0, 10) contains [0, 10] -> false (0과 10이 open range에서 제외됨)
        val open = openOpenRangeOf(0, 10)
        val closedSame = closedClosedRangeOf(0, 10)
        open.contains(closedSame).shouldBeFalse()

        // (0, 10) contains (0, 10) -> true
        open.contains(open).shouldBeTrue()

        // [0, 10] contains [0, 10] -> true
        closed.contains(closedSame).shouldBeTrue()

        // [0, 10) contains [0, 10] -> false (상한 10이 제외됨)
        val closedOpen = closedOpenRangeOf(0, 10)
        closedOpen.contains(closedSame).shouldBeFalse()

        // [0, 10] contains [0, 10) -> true
        closed.contains(closedOpen).shouldBeTrue()
    }

    @Test
    fun `overlaps with same type ranges`() {
        // [0, 5] overlaps [5, 10] -> true (5에서 만남)
        closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(5, 10)).shouldBeTrue()

        // [0, 5] overlaps [6, 10] -> false (겹치지 않음)
        closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(6, 10)).shouldBeFalse()

        // [0, 5] overlaps [3, 8] -> true (부분 겹침)
        closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(3, 8)).shouldBeTrue()

        // (0, 5) overlaps (3, 10) -> true
        openOpenRangeOf(0, 5).overlaps(openOpenRangeOf(3, 10)).shouldBeTrue()

        // (0, 5) overlaps (5, 10) -> false (5가 양쪽에서 제외)
        openOpenRangeOf(0, 5).overlaps(openOpenRangeOf(5, 10)).shouldBeFalse()
    }

    @Test
    fun `overlaps with cross-type ranges`() {
        // [0, 5] overlaps (5, 10] -> false (5가 open side에서 제외)
        closedClosedRangeOf(0, 5).overlaps(openClosedRangeOf(5, 10)).shouldBeFalse()

        // [0, 5] overlaps [5, 10) -> true (5에서 만남, 양쪽 inclusive)
        closedClosedRangeOf(0, 5).overlaps(closedOpenRangeOf(5, 10)).shouldBeTrue()

        // [0, 5) overlaps [5, 10] -> false (5가 왼쪽에서 제외)
        closedOpenRangeOf(0, 5).overlaps(closedClosedRangeOf(5, 10)).shouldBeFalse()

        // [0, 5) overlaps (4, 10] -> true (4~5 사이에서 겹침)
        closedOpenRangeOf(0, 5).overlaps(openClosedRangeOf(4, 10)).shouldBeTrue()
    }
}