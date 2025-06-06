package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.weeks
import io.bluetape4k.support.assertPositiveNumber
import java.time.ZonedDateTime

/**
 * [year]로부터 [yearCount] 만큼의 [YearRange] 시퀀스를 생성합니다.
 *
 * ```
 * val years = yearRanges(2021, 3) // 2021, 2022, 2023
 * ```
 */
fun yearRanges(
    year: Int,
    yearCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<YearRange> {
    return object: Sequence<YearRange> {
        override fun iterator(): Iterator<YearRange> = object: Iterator<YearRange> {
            private var current = YearRange(year, calendar)
            private var count = 0

            override fun hasNext(): Boolean = count < yearCount

            override fun next(): YearRange {
                if (!hasNext()) throw NoSuchElementException("No more years in the range")
                val result = current
                current = current.nextYear()
                count++
                return result
            }
        }
    }
}

/**
 * [startTime]으로부터 [quarterCount] 만큼의 [QuarterRange] 시퀀스를 생성합니다.
 *
 * ```
 * val quarters = quarterRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun quarterRanges(
    startTime: ZonedDateTime,
    quarterCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<QuarterRange> {
    return object: Sequence<QuarterRange> {
        override fun iterator(): Iterator<QuarterRange> = object: Iterator<QuarterRange> {
            private var current = QuarterRange(startTime, calendar)
            private var count = 0

            override fun hasNext(): Boolean = count < quarterCount

            override fun next(): QuarterRange {
                if (!hasNext()) throw NoSuchElementException("No more quarters in the range")
                val result = current
                current = current.nextQuarter()
                count++
                return result
            }
        }
    }
}

/**
 * [startTime]으로부터 [monthCount] 만큼의 [MonthRange] 시퀀스를 생성합니다.
 *
 * ```
 * val months = monthRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun monthRanges(
    startTime: ZonedDateTime,
    monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MonthRange> {
    monthCount.assertPositiveNumber("monthCount")
    return monthRanges(startTime, startTime + monthCount.monthPeriod(), calendar)
}

/**
 * [startTime]으로부터 [end]까지의 [MonthRange] 시퀀스를 생성합니다.
 *
 * ```
 * val months = monthRanges(ZonedDateTime.now(), ZonedDateTime.now().plusMonths(3))
 * ```
 */
fun monthRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MonthRange> {
    return object: Sequence<MonthRange> {
        override fun iterator(): Iterator<MonthRange> = object: Iterator<MonthRange> {
            private var current = MonthRange(start, calendar)

            override fun hasNext(): Boolean = current.end <= end

            override fun next(): MonthRange {
                if (!hasNext()) throw NoSuchElementException("No more months in the range")
                val result = current
                current = current.nextMonth()
                return result
            }
        }
    }
}

/**
 * [start]로부터 [weekCount] 만큼의 [WeekRange] 시퀀스를 생성합니다.
 *
 * ```
 * val weeks = weekRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun weekRanges(
    start: ZonedDateTime,
    weekCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<WeekRange> {
    weekCount.assertPositiveNumber("weekCount")
    return weekRanges(start, start + weekCount.weeks(), calendar)
}

/**
 * [start]로부터 [end]까지의 [WeekRange] 시퀀스를 생성합니다.
 *
 * ```
 * val weeks = weekRanges(ZonedDateTime.now(), ZonedDateTime.now().plusWeeks(3))
 * ```
 */
fun weekRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<WeekRange> {
    return object: Sequence<WeekRange> {
        override fun iterator(): Iterator<WeekRange> = object: Iterator<WeekRange> {
            private var current = WeekRange(start, calendar)

            override fun hasNext(): Boolean = current.end <= end

            override fun next(): WeekRange {
                if (!hasNext()) throw NoSuchElementException("No more weeks in the range")
                val result = current
                current = current.nextWeek()
                return result
            }
        }
    }
}

/**
 * [start]로부터 [dayCount] 만큼의 [DayRange] 시퀀스를 생성합니다.
 *
 * ```
 * val days = dayRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun dayRanges(
    start: ZonedDateTime,
    dayCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<DayRange> {
    dayCount.assertPositiveNumber("dayCount")
    return dayRanges(start, start + dayCount.days(), calendar)
}

/**
 * [start]로부터 [end]까지의 [DayRange] 시퀀스를 생성합니다.
 *
 * ```
 * val days = dayRanges(ZonedDateTime.now(), ZonedDateTime.now().plusDays(3))
 * ```
 */
fun dayRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<DayRange> {
    return object: Sequence<DayRange> {
        override fun iterator(): Iterator<DayRange> = object: Iterator<DayRange> {
            private var current = DayRange(start, calendar)

            override fun hasNext(): Boolean = current.end <= end

            override fun next(): DayRange {
                if (!hasNext()) throw NoSuchElementException("No more days in the range")
                val result = current
                current = current.nextDay()
                return result
            }
        }
    }
}

/**
 * [start]로부터 [hourCount] 만큼의 [HourRange] 시퀀스를 생성합니다.
 *
 * ```
 * val hours = hourRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun hourRanges(
    start: ZonedDateTime,
    hourCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<HourRange> {
    hourCount.assertPositiveNumber("hourCount")
    return hourRanges(start, start + hourCount.hours(), calendar)
}

/**
 * [start]로부터 [end]까지의 [HourRange] 시퀀스를 생성합니다.
 *
 * ```
 * val hours = hourRanges(ZonedDateTime.now(), ZonedDateTime.now().plusHours(3))
 * ```
 */
fun hourRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<HourRange> {
    return object: Sequence<HourRange> {
        override fun iterator(): Iterator<HourRange> = object: Iterator<HourRange> {
            private var current = HourRange(start, calendar)

            override fun hasNext(): Boolean = current.end <= end

            override fun next(): HourRange {
                if (!hasNext()) throw NoSuchElementException("No more hours in the range")
                val result = current
                current = current.nextHour()
                return result
            }
        }
    }
}

/**
 * [start]로부터 [minuteCount] 만큼의 [MinuteRange] 시퀀스를 생성합니다.
 *
 * ```
 * val minutes = minuteRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun minuteRanges(
    start: ZonedDateTime,
    minuteCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MinuteRange> {
    minuteCount.assertPositiveNumber("minuteCount")
    return minuteRanges(start, start + minuteCount.minutes(), calendar)
}

/**
 * [start]로부터 [end]까지의 [MinuteRange] 시퀀스를 생성합니다.
 *
 * ```
 * val minutes = minuteRanges(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(3))
 * ```
 */
fun minuteRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MinuteRange> {
    return object: Sequence<MinuteRange> {
        override fun iterator(): Iterator<MinuteRange> = object: Iterator<MinuteRange> {
            private var current = MinuteRange(start, calendar)

            override fun hasNext(): Boolean = current.end <= end

            override fun next(): MinuteRange {
                if (!hasNext()) throw NoSuchElementException("No more minutes in the range")
                val result = current
                current = current.nextMinute()
                return result
            }
        }
    }
}
