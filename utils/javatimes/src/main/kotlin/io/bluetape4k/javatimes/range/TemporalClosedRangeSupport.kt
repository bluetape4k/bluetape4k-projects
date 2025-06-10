package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.support.assertPositiveNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * Create [TemporalClosedRange] instance
 * @param start T
 * @param endInclusive T
 * @return TemporalClosedRange<T>
 */
fun <T> temporalClosedRangeOf(start: T, endInclusive: T): TemporalClosedRange<T> where T: Temporal, T: Comparable<T> {
    assert(start !is LocalDate) { "LocalDate는 지원하지 않습니다." }
    assert(endInclusive !is LocalDate) { "LocalDate는 지원하지 않습니다." }

    return TemporalClosedRange.fromClosedRange(start, endInclusive)
}

/**
 * 두 개의 [Temporal]을 이용하여 [TemporalClosedRange]를 빌드합니다.
 */
operator fun <T> T.rangeTo(endInclusive: T): TemporalClosedRange<T> where T: Temporal, T: Comparable<T> =
    temporalClosedRangeOf(this, endInclusive)

internal val SupportChronoUnits: Array<ChronoUnit> =
    arrayOf(
        ChronoUnit.YEARS,
        ChronoUnit.MONTHS,
        ChronoUnit.WEEKS,
        ChronoUnit.DAYS,
        ChronoUnit.HOURS,
        ChronoUnit.MINUTES,
        ChronoUnit.SECONDS,
        ChronoUnit.MILLIS
    )

/**
 * [step]에 의해 단계를 증가시키면서, [size]만큼의 요소들을 묶어서 리스트로 제공한다. (Scala의 sliding과 같은 기능)
 *
 * @receiver TemporalClosedRange<T>
 * @param size Int windowing 할 요소 수
 * @param step Int windowing step
 * @param unit ChronoUnit 기간 단위
 * @return Sequence<List<T>>
 */
@Suppress("UNCHECKED_CAST")
fun <T> TemporalClosedRange<T>.windowed(
    size: Int,
    step: Int = 1,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Sequence<List<T>>
        where T: Temporal, T: Comparable<T> {
    size.assertPositiveNumber("size")
    step.assertPositiveNumber("step")
    assert(SupportChronoUnits.contains(unit)) { "Not supoorted ChronoUnit. unit=$unit" }

    return sequence {
        var current: T = start.startOf(unit)
        val increment = step.temporalAmount(unit)

        while (current <= endInclusive) {
            val item = List(size) { (current + it.temporalAmount(unit)) as T }
                .takeWhile { it <= endInclusive }
            yield(item)
            current = (current + increment) as T
        }
    }
}

fun <T> TemporalClosedRange<T>.windowedYears(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.YEARS)

fun <T> TemporalClosedRange<T>.windowedMonths(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MONTHS)

fun <T> TemporalClosedRange<T>.windowedWeeks(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.WEEKS)

fun <T> TemporalClosedRange<T>.windowedDays(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.DAYS)

fun <T> TemporalClosedRange<T>.windowedHours(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.HOURS)

fun <T> TemporalClosedRange<T>.windowedMinutes(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MINUTES)

fun <T> TemporalClosedRange<T>.windowedSeconds(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.SECONDS)

fun <T> TemporalClosedRange<T>.windowedMillis(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MILLIS)

/**
 * 기간을 `chronoUnit` 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize chunk Size (require positive number)
 * @return Chunk 된 기간들의 Sequence
 */
fun <T> TemporalClosedRange<T>.chunked(
    chunkSize: Int,
    chronoUnit: ChronoUnit,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(chunkSize, chunkSize, chronoUnit)

/**
 * 기간을 년 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 년씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedYears(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.YEARS)

/**
 * 기간을 월 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 월씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedMonths(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.MONTHS)

/**
 * 기간을 주(week) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 주(week) 씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedWeeks(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.WEEKS)


/**
 * 기간을 일(day) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 일(day)씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedDays(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.DAYS)

/**
 * 기간을 시(hour) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 시(hour)씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedHours(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.HOURS)

/**
 * 기간을 분(minutes) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 분(minute)씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedMinutes(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.MINUTES)

/**
 * 기간을 초(second) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize Int chunk Size
 * @return Sequence<List<T>> N 초(second)씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedSeconds(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.SECONDS)

/**
 * 기간을 밀리초(millisecond) 단위의 Sequence로 chunk 합니다.
 *
 * @receiver TemporalClosedRange<T>
 * @param chunkSize chunk Size
 * @return N 밀리초(millisecond)씩 나뉜 Sequence
 */
fun <T> TemporalClosedRange<T>.chunkedMillis(chunkSize: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(chunkSize, ChronoUnit.MILLIS)

/**
 * 현재 요소와 다음 요소를 [Pair]로 만들어 Sequence를 제공한다
 *
 * @receiver TemporalClosedRange<T>
 * @param unit ChronoUnit
 * @return Sequence<Pair<T, T>>
 */
@Suppress("UNCHECKED_CAST")
fun <T> TemporalClosedRange<T>.zipWithNext(unit: ChronoUnit): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> {
    assert(unit in SupportChronoUnits) { "Not supported ChronoUnit. unit=$unit" }

    return sequence {
        var current: T = start.startOf(unit)
        val increment = 1.temporalAmount(unit)
        val limit = (endInclusive - increment) as T

        while (current <= limit) {
            val second = (current + increment) as T
            yield(current to second)
            current = second
        }
    }
}

fun <T> TemporalClosedRange<T>.zipWithNextYear(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.YEARS)

fun <T> TemporalClosedRange<T>.zipWithNextMonth(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MONTHS)

fun <T> TemporalClosedRange<T>.zipWithNextWeek(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.WEEKS)

fun <T> TemporalClosedRange<T>.zipWithNextDay(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.DAYS)

fun <T> TemporalClosedRange<T>.zipWithNextHour(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.HOURS)

fun <T> TemporalClosedRange<T>.zipWithNextMinute(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MINUTES)

fun <T> TemporalClosedRange<T>.zipWithNextSecond(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.SECONDS)

fun <T> TemporalClosedRange<T>.zipWithNextMilli(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MILLIS)
