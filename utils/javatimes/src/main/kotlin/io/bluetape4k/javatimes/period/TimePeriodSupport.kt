package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.DaysPerWeek
import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.max
import io.bluetape4k.javatimes.min
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.startOfDay
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.javatimes.startOfMinute
import io.bluetape4k.javatimes.startOfMonth
import io.bluetape4k.javatimes.startOfQuarter
import io.bluetape4k.javatimes.startOfSecond
import io.bluetape4k.javatimes.startOfWeek
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.javatimes.yearPeriod
import io.bluetape4k.support.assertZeroOrPositiveNumber
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.Temporal

/**
 * 두 개의 시각을 비교하여 더 작은 시각과 더 큰 시각 순서의 Pair를 반환합니다.
 */
fun <T> adjustPeriod(left: T?, right: T?): Pair<T?, T?> where T: Temporal, T: Comparable<T> =
    Pair(left min right, left max right)


/**
 * 시작시각과 기간을 조정하여 시작시각과 Positive 기간을 반환합니다.
 */
fun adjustPeriod(start: ZonedDateTime, duration: Duration): Pair<ZonedDateTime, Duration> = when {
    duration.isPositive -> Pair(start, duration)
    else                -> Pair(start - duration, duration.negated())
}

/**
 * [start]와 [end] 가 시간 순으로 되어 있는지 확인홥니다.
 */
fun assertValidPeriod(start: ZonedDateTime?, end: ZonedDateTime?) {
    if (start != null && end != null) {
        assert(start <= end) { "시작시각이 완료시각 이전이어야 합니다. start=$start, end=$end" }
    }
}

/**
 * [left]의 요소인 기간 [ITimePeriod] 과 [right]의 요소인 기간 [ITimePeriod] 가 모두 같은지 확인합니다.
 *
 * ```
 * val left = listOf(TimeRange(start1, end1), TimeRange(start2, end2))
 * val right = listOf(TimeRange(start1, end1), TimeRange(start2, end2))
 *
 * left.allItemsAreEquals(right) // true
 * ```
 *
 * @param left 비교할 첫 번째 기간들
 * @param right 비교할 두 번째 기간들
 * @return 모두 같으면 true, 아니면 false
 */
fun <T: ITimePeriod> allItemsAreEquals(left: Iterable<T>, right: Iterable<T>): Boolean {
    val leftIter = left.iterator()
    val rightIter = right.iterator()

    while (leftIter.hasNext() && rightIter.hasNext()) {
        if (!leftIter.next().isSamePeriod(rightIter.next())) {
            return false
        }
    }
    return !leftIter.hasNext() && !rightIter.hasNext()
}

/**
 * [ZonedDateTime]과 [duration]을 이용하여 [TimeBlock]을 생성합니다.
 */
fun ZonedDateTime.toTimeBlock(duration: Duration): ITimeBlock = TimeBlock(this, duration)

/**
 * [ZonedDateTime]과 [end]를 이용하여 [TimeBlock]을 생성합니다.
 */
fun ZonedDateTime.toTimeBlock(end: ZonedDateTime): ITimeBlock = TimeBlock(this, end)

/**
 * [ZonedDateTime]과 [duration]을 이용하여 [TimeRange]을 생성합니다.
 */
fun ZonedDateTime.toTimeRange(duration: Duration): ITimeRange = TimeRange(this, duration)

/**
 * [ZonedDateTime]과 [end]를 이용하여 [TimeRange]을 생성합니다.
 */
fun ZonedDateTime.toTimeRange(end: ZonedDateTime): ITimeRange = TimeRange(this, end)

/**
 * [calendar]의 기준 월을 기준으로 [year]와 [monthOfYear]를 이용하여 연도를 계산합니다.
 * 회계 년도 같은 경우 2월이 기준월이므로, 1월이 기준월인 경우에는 [year]-1 을 반환합니다.
 */
fun yearOf(year: Int, monthOfYear: Int, calendar: ITimeCalendar = TimeCalendar.Default): Int = when {
    monthOfYear in 1..12             -> year
    monthOfYear < calendar.baseMonth -> year - 1
    monthOfYear > 12                 -> year + 1
    else                             -> throw IllegalArgumentException("Invalid monthOfYear[$monthOfYear]")
}

fun ZonedDateTime.yearOf(): Int = yearOf(year, monthValue)

/**
 * [year] 부터 [yearCount] 만큼의 기간을 가진 [ITimeRange]를 생성합니다.
 */
fun relativeYearPeriodOf(year: Int, yearCount: Int = 1): ITimeRange {
    yearCount.assertZeroOrPositiveNumber("yearCount")

    val start = startOfYear(year)
    return TimeRange(start, start + yearCount.yearPeriod())
}

/**
 * 현 시각으로부터 [yearCount]만큼의 년 기간을 가진 [ITimeRange]를 생성합니다.
 */
fun ZonedDateTime.relativeYearPeriod(yearCount: Int): ITimeRange {
    yearCount.assertZeroOrPositiveNumber("yearCount")

    val start = this.startOfYear()
    return TimeRange(start, start + yearCount.yearPeriod())
}

/**
 * 현 시각으로부터 분기 수([quarterCount]) 만큼의 분기 기간을 가진 [ITimeRange]를 생성합니다.
 */
fun ZonedDateTime.relativeQuarterPeriod(quarterCount: Int = 1): ITimeRange {
    quarterCount.assertZeroOrPositiveNumber("quarterCount")

    val start = this.startOfQuarter()
    val months = quarterCount * MonthsPerQuarter
    return TimeRange(start, start.plusMonths(months.toLong()))
}

/**
 * 현 시각으로부터 [monthCount]만큼의 월 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndMonthRange = now.relativeMonthPeriod(3)  // 현재 월부터 3개월 후까지의 기간
 * ```
 */
fun ZonedDateTime.relativeMonthPeriod(monthCount: Int): ITimeRange {
    monthCount.assertZeroOrPositiveNumber("monthCount")

    val start = this.startOfMonth()
    return TimeRange(start, start + monthCount.monthPeriod())
}

/**
 * 현 시각으로부터 [weekCount]만큼의 주 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndWeekRange = now.relativeWeekPeriod(3)  // 현재 주부터 3주 후까지의 기간
 */
fun ZonedDateTime.relativeWeekPeriod(weekCount: Int = 1): ITimeRange {
    weekCount.assertZeroOrPositiveNumber("weekCount")

    val start = this.startOfWeek()
    return TimeRange(start, start.plusDays(weekCount.toLong() * DaysPerWeek))
}

/**
 * 현 시각으로부터 [dayCount]만큼의 일 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndDayRange = now.relativeDayPeriod(3)  // 현재 시각부터 3일 후까지의 기간
 * ```
 */
fun ZonedDateTime.relativeDayPeriod(dayCount: Int = 1): TimeRange {
    dayCount.assertZeroOrPositiveNumber("dayCount")

    val start = this.startOfDay()
    return TimeRange(start, start.plusDays(dayCount.toLong()))
}

/**
 * 현 시각으로부터 [hourCount]만큼의 시간 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndHourRange = now.relativeHourPeriod(3)  // 현재 시각부터 3시간 후까지의 기간
 * ```
 */
fun ZonedDateTime.relativeHourPeriod(hourCount: Int = 1): TimeRange {
    hourCount.assertZeroOrPositiveNumber("hourCount")

    val start = this.startOfHour()
    return TimeRange(start, start.plusHours(hourCount.toLong()))
}

/**
 * 현 시각으로부터 [minuteCount]만큼의 분 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndMinuteRange = now.relativeMinutePeriod(3)  // 현재 시각부터 3분 후까지의 기간
 * ```
 */
fun ZonedDateTime.relativeMinutePeriod(minuteCount: Int = 1): TimeRange {
    minuteCount.assertZeroOrPositiveNumber("minuteCount")

    val start = this.startOfMinute()
    return TimeRange(start, start.plusMinutes(minuteCount.toLong()))
}

/**
 * 현 시각으로부터 [secondCount]만큼의 초 기간을 가진 [ITimeRange]를 생성합니다.
 *
 * ```
 * val now = ZonedDateTime.now()
 * val nowAndSecondRange = now.relativeSecondPeriod(3)  // 현재 시각부터 3초 후까지의 기간
 * ```
 */
fun ZonedDateTime.relativeSecondPeriod(secondCount: Int = 1): TimeRange {
    secondCount.assertZeroOrPositiveNumber("secondCount")

    val start = this.startOfSecond()
    return TimeRange(start, start.plusSeconds(secondCount.toLong()))
}

/**
 * 현 기간이 [moment]를 포함하는지 여부
 */
@Suppress("ConvertTwoComparisonsToRangeCheck")
infix fun ITimePeriod.hasInsideWith(moment: ZonedDateTime): Boolean =
    start <= moment && moment <= end

/**
 * 현 기간이 [that] 기간을 포함하는지 여부
 */
infix fun ITimePeriod.hasInsideWith(that: ITimePeriod): Boolean =
    this.hasInsideWith(that.start) && this.hasInsideWith(that.end)

/**
 * [moment]가 현 기간의 내부에 완전히 포함되는지 여부
 */
infix fun ITimePeriod.hasPureInsideWith(moment: ZonedDateTime): Boolean =
    start < moment && moment < end

/**
 * 두 기간 중 하나가 다른 기간에 완전히 포함되는지 여부
 */
infix fun ITimePeriod.hasPureInsideWith(that: ITimePeriod): Boolean =
    hasPureInsideWith(that.start) && hasPureInsideWith(that.end)

/**
 * 두 기간의 관계를 비교하여 [PeriodRelation]을 반환합니다.
 *
 * ```
 * val period1 = TimeRange(start1, end1)
 * val period2 = TimeRange(start2, end2)
 * val relation = period1 relationWith period2
 * // After, Before, ExactMatch, StartTouching, EndTouching, EnclosingStartTouching, EnclosingEndTouching, Enclosing, InsideStartTouching, InsideEndTouching, Inside, StartInside, EndInside, NoRelation
 * ```
 */
infix fun ITimePeriod.relationWith(that: ITimePeriod): PeriodRelation = when {
    this.start > that.end    -> PeriodRelation.After
    this.end < that.start    -> PeriodRelation.Before
    this.isSamePeriod(that)  -> PeriodRelation.ExactMatch
    this.start == that.end   -> PeriodRelation.StartTouching
    this.end == that.start   -> PeriodRelation.EndTouching

    this.hasInsideWith(that) -> when {
        this.start == that.start -> PeriodRelation.EnclosingStartTouching
        this.end == that.end     -> PeriodRelation.EnclosingEndTouching
        else                     -> PeriodRelation.Enclosing
    }

    else                     -> {
        val isInsideStart = that.hasInsideWith(this.start)
        val isInsideEnd = that.hasInsideWith(this.end)
        when {
            isInsideStart && isInsideEnd -> when {
                this.start == that.start -> PeriodRelation.InsideStartTouching
                this.end == that.end     -> PeriodRelation.InsideEndTouching
                else                     -> PeriodRelation.Inside
            }

            isInsideStart                -> PeriodRelation.StartInside
            isInsideEnd                  -> PeriodRelation.EndInside
            else                         -> PeriodRelation.NoRelation
        }
    }
}


/**
 * 두 기간의 교집합 기간이 있는지 확인합니다.
 */
infix fun ITimePeriod.intersectWith(that: ITimePeriod): Boolean =
    hasInsideWith(that.start) || hasInsideWith(that.end) || that.hasPureInsideWith(this)

/**
 * 두 기간이 겹치는 기간이 있는지 확인합니다.
 */
infix fun ITimePeriod.overlapWith(that: ITimePeriod): Boolean =
    this.relationWith(that) !in PeriodRelation.NotOverlappedRelations

/**
 * 두 기간이 겹치는 기간을 계산하여 [ITimeBlock]으로 반환합니다.
 */
infix fun ITimePeriod.intersectBlock(that: ITimePeriod): ITimeBlock? {
    var intersection: ITimeBlock? = null

    if (this.intersectWith(that)) {
        val start = this.start max that.start
        val end = this.end min that.end
        intersection = TimeBlock(start, end, this.readonly)
    }

    return intersection
}

/**
 * 두 기간이 겹치는 기간을 계산하여 [TimeRange]으로 반환합니다.
 */
infix fun ITimePeriod.intersectRange(that: ITimePeriod): TimeRange? {
    var intersection: TimeRange? = null

    if (this.intersectWith(that)) {
        val start = this.start max that.start
        val end = this.end min that.end
        intersection = TimeRange(start, end, this.readonly)
    }

    return intersection
}

/**
 * 두 기간의 합집합 기간을 계산하여 [ITimeBlock]으로 반환합니다.
 */
infix fun ITimePeriod.unionBlock(that: ITimePeriod): ITimeBlock {
    val start = this.start min that.start
    val end = this.end max that.end

    return TimeBlock(start, end, this.readonly)
}

/**
 * 두 기간의 합집합 기간을 계산하여 [TimeRange]으로 반환합니다.
 */
infix fun ITimePeriod.unionRange(that: ITimePeriod): TimeRange {
    val start = this.start min that.start
    val end = this.end max that.end

    return TimeRange(start!!, end!!, this.readonly)
}
