package io.bluetape4k.javatimes.interval

import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.support.assertPositiveNumber
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * TemporalInterval 에서 제공하는 [ChronoUnit]
 */
private val SupportChronoUnits =
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
 * 기간을 [unit] 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val chunked = interval.chunked(3, ChronoUnit.DAYS) // 3일 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunked(
    size: Int,
    unit: ChronoUnit,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, size, unit)

/**
 * 기간을 년 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusYears(10))
 * val chunked = interval.chunkYears(3) // 3년 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkYears(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.YEARS)

/**
 * 기간을 월 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMonths(10))
 * val chunked = interval.chunkMonths(3) // 3개월 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkMonths(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.MONTHS)

/**
 * 기간을 주 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusWeeks(10))
 * val chunked = interval.chunkWeeks(3) // 3주 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkWeeks(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.WEEKS)

/**
 * 기간을 일 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val chunked = interval.chunkDays(3) // 3일 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkDays(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.DAYS)

/**
 * 기간을 시간 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusHours(10))
 * val chunked = interval.chunkHours(3) // 3시간 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkHours(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.HOURS)

/**
 * 기간을 분 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(10))
 * val chunked = interval.chunkMinutes(3) // 3분 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkMinutes(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.MINUTES)

/**
 * 기간을 초 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusSeconds(10))
 * val chunked = interval.chunkSeconds(3) // 3초 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkSeconds(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.SECONDS)

/**
 * 기간을 밀리초 단위로 [size] 크기로 나눈 결과를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMillis(10))
 * val chunked = interval.chunkMillis(3) // 3밀리초 단위로 나눈 결과
 * ```
 */
fun <T> ReadableTemporalInterval<T>.chunkMillis(size: Int): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    chunked(size, ChronoUnit.MILLIS)

//
// windowed
//

/**
 * 기간을 [unit] 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val windowed = interval.windowed(3, 1, ChronoUnit.DAYS) // 3일 단위로 1일씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @param unit window 단위
 * @return Sequence<List<T>>
 */
@Suppress("UNCHECKED_CAST")
fun <T> ReadableTemporalInterval<T>.windowed(
    size: Int,
    step: Int = 1,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> {
    size.assertPositiveNumber("size")
    step.assertPositiveNumber("step")
    assert(unit in SupportChronoUnits) { "Not supported ChronoUnit. unit=$unit" }

    return sequence {
        var current: T = startInclusive.startOf(unit)
        val increment = step.temporalAmount(unit)

        while (current < endExclusive) {
            val item = List(size) {
                (current + it.temporalAmount(unit)) as T
            }.takeWhile { it < endExclusive }

            yield(item)
            current = (current + increment) as T
        }
    }
}

/**
 * 기간을 년 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusYears(10))
 * val windowed = interval.windowedYears(3, 1) // 3년 단위로 1년씩 증가하는 window
 * ```
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedYears(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.YEARS)

/**
 * 기간을 월 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMonths(10))
 * val windowed = interval.windowedMonths(3, 1) // 3개월 단위로 1개월씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedMonths(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MONTHS)

/**
 * 기간을 주 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusWeeks(10))
 * val windowed = interval.windowedWeeks(3, 1) // 3주 단위로 1주씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedWeeks(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.WEEKS)

/**
 * 기간을 일 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val windowed = interval.windowedDays(3, 1) // 3일 단위로 1일씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedDays(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.DAYS)

/**
 * 기간을 시간 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusHours(10))
 * val windowed = interval.windowedHours(3, 1) // 3시간 단위로 1시간씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedHours(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.HOURS)

/**
 * 기간을 분 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(10))
 * val windowed = interval.windowedMinutes(3, 1) // 3분 단위로 1분씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedMinutes(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MINUTES)

/**
 * 기간을 초 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusSeconds(10))
 * val windowed = interval.windowedSeconds(3, 1) // 3초 단위로 1초씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedSeconds(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.SECONDS)

/**
 * 기간을 밀리초 단위로 [size] 크기로 [step] 단계로 증가시킨 window를 Sequence 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMillis(10))
 * val windowed = interval.windowedMillis(3, 1) // 3밀리초 단위로 1밀리초씩 증가하는 window
 * ```
 *
 * @param size window 크기
 * @param step window 증가 단계
 * @return Sequence<List<T>>
 */
fun <T> ReadableTemporalInterval<T>.windowedMillis(
    size: Int,
    step: Int = 1,
): Sequence<List<T>> where T: Temporal, T: Comparable<T> =
    windowed(size, step, ChronoUnit.MILLIS)


/**
 * 기간을 [unit] 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val zipped = interval.zipWithNext(ChronoUnit.DAYS)
 * // [2021-01-01T00:00:00Z, 2021-01-02T00:00:00Z], [2021-01-02T00:00:00Z, 2021-01-03T00:00:00Z], ...
 * ```
 *
 * @param unit ChronoUnit 구분 단위 ([ChronoUnit])
 */
@Suppress("UNCHECKED_CAST")
fun <T> ReadableTemporalInterval<T>.zipWithNext(unit: ChronoUnit): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> {
    assert(unit in SupportChronoUnits) { "Not supported ChronoUnit. unit=$unit" }

    return sequence {
        var current: T = startInclusive.startOf(unit)
        val increment = 1.temporalAmount(unit)
        val limit: T = (endExclusive - increment) as T

        while (current < limit) {
            val next = (current + increment) as T
            yield(current to next)
            current = next
        }
    }
}

/**
 * 기간을 년 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusYears(10))
 * val zipped = interval.zipWithNextYear()
 * // [2021-01-01T00:00:00Z, 2022-01-01T00:00:00Z], [2022-01-01T00:00:00Z, 2023-01-01T00:00:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextYear(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.YEARS)

/**
 * 기간을 월 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMonths(10))
 * val zipped = interval.zipWithNextMonth()
 * // [2021-01-01T00:00:00Z, 2021-02-01T00:00:00Z], [2021-02-01T00:00:00Z, 2021-03-01T00:00:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextMonth(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MONTHS)

/**
 * 기간을 주 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusWeeks(10))
 * val zipped = interval.zipWithNextWeek()
 * // [2021-01-01T00:00:00Z, 2021-01-08T00:00:00Z], [2021-01-08T00:00:00Z, 2021-01-15T00:00:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextWeek(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.WEEKS)

/**
 * 기간을 일 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusDays(10))
 * val zipped = interval.zipWithNextDay()
 * // [2021-01-01T00:00:00Z, 2021-01-02T00:00:00Z], [2021-01-02T00:00:00Z, 2021-01-03T00:00:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextDay(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.DAYS)

/**
 * 기간을 시간 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusHours(10))
 * val zipped = interval.zipWithNextHour()
 * // [2021-01-01T00:00:00Z, 2021-01-01T01:00:00Z], [2021-01-01T01:00:00Z, 2021-01-01T02:00:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextHour(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.HOURS)

/**
 * 기간을 분 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(10))
 * val zipped = interval.zipWithNextMinute()
 * // [2021-01-01T00:00:00Z, 2021-01-01T00:01:00Z], [2021-01-01T00:01:00Z, 2021-01-01T00:02:00Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextMinute(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MINUTES)

/**
 * 기간을 초 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusSeconds(10))
 * val zipped = interval.zipWithNextSecond()
 * // [2021-01-01T00:00:00Z, 2021-01-01T00:00:01Z], [2021-01-01T00:00:01Z, 2021-01-01T00:00:02Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextSecond(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.SECONDS)

/**
 * 기간을 밀리초 단위의 다음 기간을 Pair 로 반환합니다.
 *
 * ```
 * val interval = ZonedDateTimeInterval(ZonedDateTime.now(), ZonedDateTime.now().plusMillis(10))
 * val zipped = interval.zipWithNextMilli()
 * // [2021-01-01T00:00:00Z, 2021-01-01T00:00:00.001Z], [2021-01-01T00:00:00.001Z, 2021-01-01T00:00:00.002Z], ...
 * ```
 */
fun <T> ReadableTemporalInterval<T>.zipWithNextMilli(): Sequence<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNext(ChronoUnit.MILLIS)
