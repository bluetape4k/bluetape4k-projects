# Module bluetape4k-javatimes

English | [한국어](./README.ko.md)

An advanced time-operations library for the Java Time API (java.time). Supports complex time-related tasks including Temporal Intervals, a Period Framework, and Temporal Ranges.

## Overview

`bluetape4k-javatimes` builds on top of the foundational time DSL in `bluetape4k-core` (`io.bluetape4k.javatimes` package) to provide higher-level time operations: Joda-Time-style Intervals, business-day calculations, calendar ranges, and Flow-based time-series processing.

> **Note**: Basic extension functions for Duration/Period DSL, Instant/LocalDateTime/ZonedDateTime creation, and Quarters are included in `bluetape4k-core` (`io.bluetape4k.javatimes`) and can be used with just the core dependency.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-javatimes:${bluetape4kVersion}")

    // Optional coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
}
```

## Core Features (provided by bluetape4k-core)

The following features are in `bluetape4k-core`'s `io.bluetape4k.javatimes` package and are available with just the core dependency. Since `javatimes` depends on `core`, they are always available.

- **Duration/Period DSL**: `5.days()`, `3.hours()`, `2.yearPeriod()`, etc.
- **Duration utilities**: `durationOfDay()`, `formatHMS()`, `formatISO()`, etc.
- **Common temporal extensions**: `startOfYear()`, `startOfMonth()`, `firstOfMonth`, `toEpochMillis()`, etc.
- **Instant/LocalDateTime/ZonedDateTime creation**: `nowInstant()`, `localDateOf()`, `zonedDateTimeOf()`, etc.
- **TemporalAccessor formatting**: `toIsoInstantString()`, `toIsoDateString()`, etc.
- **Quarter support**: `Quarter.Q1`, `YearQuarter(2024, Quarter.Q1)`, etc.

See the `bluetape4k-core` module README for details.

## Features (this module)

### Temporal Interval (interval/)

Joda-Time-style time interval support.

```kotlin
// Create an interval
val start = nowInstant()
val end = start + 1.days()
val interval = temporalIntervalOf(start, end)

// Create from a Duration
val interval2 = temporalIntervalOf(start, 2.hours())

// Check containment
val someInstant = start + 30.minutes()
interval.contains(someInstant)  // true

// Check overlap
val otherInterval = temporalIntervalOf(start + 12.hours(), end + 12.hours())
interval.overlaps(otherInterval)  // true

// Convert to Duration
val duration = interval.toDuration()

// Windowed (sliding window)
interval.windowedYears(3, 1)    // 3-year window, move 1 year at a time
interval.windowedMonths(6, 1)   // 6-month window, move 1 month at a time
interval.windowedDays(7, 1)     // 7-day window, move 1 day at a time

// Chunked (fixed partitions)
interval.chunkYears(1)          // split into 1-year chunks
interval.chunkMonths(3)         // split into quarterly chunks
interval.chunkDays(1)           // split into daily chunks
```

### Period Framework (period/)

A framework for complex period computations and relationships.

#### TimePeriod, TimeBlock, TimeRange

```kotlin
// TimeBlock: defined by a start time and duration
val block = TimeBlock(start, 2.hours())

// TimeRange: defined by a start and end time
val range = TimeRange(start, end)

// Period manipulation
block.move(1.hours())        // shift by 1 hour
range.expandBy(30.minutes()) // expand by 30 minutes

// Period relationship
val relation = block.relationWith(otherBlock)
// PeriodRelation: Before, After, StartTouching, EndTouching,
//                 ExactMatch, Inside, Covers, Overlap, etc.
```

#### DateAdd — Business Day Calculations

Supports business day calculations that exclude weekends and holidays.

```kotlin
val dateAdd = DateAdd().apply {
    excludePeriods += TimeRange(start.startOfDay(), (start + 2.days()).startOfDay())
    excludePeriods += TimeRange(holiday.startOfDay(), (holiday + 1.days()).startOfDay())
}

// Calculate excluding the specified periods
dateAdd.add(start, 5.days())
dateAdd.subtract(start, 3.days())
```

#### DateDiff — Period Difference

```kotlin
val dateDiff = DateDiff(start, end)

dateDiff.years    // difference in years
dateDiff.months   // difference in months
dateDiff.days     // difference in days
dateDiff.hours    // difference in hours
dateDiff.minutes  // difference in minutes
dateDiff.seconds  // difference in seconds
```

#### TimeCalendar / TimeCalendarConfig

`TimeCalendar` encapsulates "calendar rules" such as start/end time mapping and the first day of the week. `TimeCalendarConfig` exposes three values:

- `startOffset`: offset applied when mapping the period start time
- `endOffset`: offset applied when mapping the period end time
- `firstDayOfWeek`: the start of the week for weekly calculations

The default configuration applies `0ns` to the start and `-1ns` to the end, representing the `[start, end)` half-open interval. Use `TimeCalendarConfig.EmptyOffset` or `TimeCalendar.EmptyOffset` for a fully inclusive range.

```kotlin
import java.time.DayOfWeek
import java.time.Duration

val calendar = TimeCalendar(
    TimeCalendarConfig(
        startOffset = Duration.ofHours(1),
        endOffset = Duration.ofHours(-1),
        firstDayOfWeek = DayOfWeek.SUNDAY,
    )
)

val range = CalendarTimeRange(
    TimeRange(
        zonedDateTimeOf(2024, 4, 1, 9, 0),
        zonedDateTimeOf(2024, 4, 1, 18, 0),
    ),
    calendar,
)

range.start         // 2024-04-01T10:00...
range.end           // 2024-04-01T17:59:59.999999999...
range.unmappedStart // 2024-04-01T09:00...
range.unmappedEnd   // 2024-04-01T18:00...
```

To reflect a custom base month for fiscal-year calculations, override `baseMonth` in a custom calendar:

```kotlin
val fiscalCalendar = object : TimeCalendar(TimeCalendarConfig()) {
    override val baseMonth: Int = 4
}

yearOf(2024, 3, fiscalCalendar)  // 2023
yearOf(2024, 4, fiscalCalendar)  // 2024
zonedDateTimeOf(2024, 3, 1).yearOf(fiscalCalendar)  // 2023
```

### Calendar Ranges (period/ranges/)

Range objects aligned to calendar units.

```kotlin
val now = nowZonedDateTime()

val yearRange   = YearRange(now)     // entire year
val monthRange  = MonthRange(now)    // entire month
val weekRange   = WeekRange(now)     // entire week (Mon–Sun)
val dayRange    = DayRange(now)      // entire day (00:00–23:59)
val hourRange   = HourRange(now)     // current hour (:00–:59)
val minuteRange = MinuteRange(now)   // current minute (:00–:59)

yearRange.year              // year number
monthRange.monthOfYear      // month number
weekRange.weekOfYear        // week-of-year number

// Collections of consecutive ranges
val months = MonthRangeCollection(now, 6)    // 6 months from now
val days   = DayRangeCollection(now, 30)     // 30 days from now

months.forEach { monthRange ->
    println("${monthRange.year}-${monthRange.monthOfYear}")
}
```

#### Coroutines Support (period/ranges/coroutines/)

Flow-based calendar range operations.

```kotlin
import kotlinx.coroutines.flow.*

flowOfYearRange(startTime, 5)      // 5 yearly ranges
    .collect { yearRange -> println(yearRange.year) }

flowOfMonthRange(startTime, 12)    // 12 monthly ranges
    .collect { monthRange -> println("${monthRange.year}-${monthRange.monthOfYear}") }

flowOfDayRange(startTime, 30)      // 30 daily ranges
    .collect { dayRange -> println(dayRange.start) }

flowOfHourRange(startTime, 24)     // 24 hourly ranges
flowOfMinuteRange(startTime, 60)   // 60 minute ranges
```

### Temporal Range (range/)

Kotlin Range-style temporal ranges.

> Note: Generic temporal ranges currently support types with epoch-millis-based iteration: `Instant`, `ZonedDateTime`, `LocalDateTime`, `OffsetDateTime`, `Date`, and `Timestamp`. `LocalDate`, `LocalTime`, and `OffsetTime` are not supported.

```kotlin
val start = zonedDateTimeOf(2024, 1, 1)
val end   = zonedDateTimeOf(2024, 12, 31)
val range = start..end

// Iterate with a step
range.step(1.monthPeriod()).forEach { time -> println(time) }
range.step(1.weekPeriod()).forEach  { time -> println(time) }

// Windowed
range.windowedYears(3, 1)    // 3-year window, step 1 year
range.windowedMonths(6, 2)   // 6-month window, step 2 months
range.windowedDays(7, 1)     // 7-day window, step 1 day

// Chunked
range.chunkedYears(1)        // 1-year chunks
range.chunkedMonths(3)       // quarterly chunks
range.chunkedDays(7)         // weekly chunks

// ZipWithNext
range.zipWithNextYear()      // (2024, 2025), (2025, 2026), ...
range.zipWithNextMonth()     // adjacent month pairs
range.zipWithNextDay()       // adjacent day pairs
```

#### Coroutines Support (range/coroutines/)

```kotlin
val range = zonedDateTimeOf(2024, 1, 1)..zonedDateTimeOf(2024, 12, 31)

range.asFlow().collect { time -> println(time) }

range.windowedFlowMonths(3)
    .collect { (start, end) -> println("$start ~ $end") }

range.chunkedFlowDays(7)
    .collect { weekRange -> println("Week: ${weekRange.first} ~ ${weekRange.last}") }

range.zipWithNextFlowDays()
    .collect { (day1, day2) -> println("$day1 -> $day2") }
```

## Usage Examples

### Business Day Calculation

```kotlin
val today = todayZonedDateTime()
val dateAdd = DateAdd()

val holidays = listOf(
    zonedDateTimeOf(2024, 1, 1),
    zonedDateTimeOf(2024, 2, 10),
    zonedDateTimeOf(2024, 3, 1),
)
holidays.forEach { holiday ->
    dateAdd.excludePeriods += TimeRange(holiday.startOfDay(), (holiday + 1.days()).startOfDay())
}

val after10BusinessDays = dateAdd.add(today, 10.days())
```

### Monthly Statistics Aggregation

```kotlin
val startDate = zonedDateTimeOf(2024, 1, 1)

val monthlyStats = MonthRangeCollection(startDate, 12)
    .map { monthRange ->
        MonthlyReport(
            year = monthRange.year,
            month = monthRange.monthOfYear,
            data = calculateStats(monthRange.start, monthRange.end)
        )
    }
```

### Time-Series Data Processing with Flow

```kotlin
val range = zonedDateTimeOf(2024, 1, 1)..zonedDateTimeOf(2024, 12, 31)

// Process data in weekly chunks
range.chunkedFlowDays(7)
    .map { weekDays -> processWeeklyData(weekDays.first(), weekDays.last()) }
    .collect { result -> println(result) }

// 3-month moving average
range.windowedFlowMonths(3)
    .map { (start, end) -> calculateMovingAverage(start, end) }
    .collect { avg -> println(avg) }
```

### Overlap Detection

```kotlin
val meeting1 = TimeBlock(zonedDateTimeOf(2024, 10, 14, 10, 0), 2.hours())
val meeting2 = TimeBlock(zonedDateTimeOf(2024, 10, 14, 11, 0), 1.hours())

when (meeting1.relationWith(meeting2)) {
    PeriodRelation.Overlap -> println("Meetings overlap")
    PeriodRelation.Before  -> println("meeting1 comes first")
    PeriodRelation.After   -> println("meeting1 comes later")
    else                   -> println("Other relation")
}
```

## Testing

```bash
./gradlew :bluetape4k-javatimes:test
./gradlew test --tests "io.bluetape4k.javatimes.DurationSupportTest"
```

## References

- [Java Time API Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
- [Joda-Time](https://www.joda.org/joda-time/) — design inspiration
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) — Kotlin multiplatform time library
