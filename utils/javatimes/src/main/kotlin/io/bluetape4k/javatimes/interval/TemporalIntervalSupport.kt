package io.bluetape4k.javatimes.interval

import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.javatimes.toEpochDay
import io.bluetape4k.support.assertPositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount


/**
 * [start]와 [endExclusive]을 이용하여 [TemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *    LocalDate.of(2021, 1, 1),
 *    LocalDate.of(2021, 1, 10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param start 시작 시각
 * @param endExclusive 완료 시각
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 */
fun <T> temporalIntervalOf(
    start: T,
    endExclusive: T,
    zoneId: ZoneId = ZoneOffset.UTC,
): TemporalInterval<T> where T: Temporal, T: Comparable<T> {
    return when {
        start <= endExclusive -> TemporalInterval(start, endExclusive, zoneId)
        else                  -> TemporalInterval(endExclusive, start, zoneId)
    }
}

/**
 * [start] 와 [duration]을 이용하여 [TemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *   LocalDate.of(2021, 1, 1),
 *   Period.ofDays(10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param start 시작 시각
 * @param duration 기간을 나타내는 정보
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 */
@Suppress("UNCHECKED_CAST")
fun <T> temporalIntervalOf(
    start: T,
    duration: TemporalAmount,
    zoneId: ZoneId = ZoneOffset.UTC,
): TemporalInterval<T> where T: Temporal, T: Comparable<T> =
    temporalIntervalOf(start, (start + duration) as T, zoneId)

/**
 * [duration]와 [endExclusive]를 이용하여 [TemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *      Period.ofDays(10),
 *      LocalDate.of(2021, 1, 10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param duration 기간을 나타내는 정보
 * @param endExclusive 완료 시각
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 */
@Suppress("UNCHECKED_CAST")
fun <T> temporalIntervalOf(
    duration: TemporalAmount,
    endExclusive: T,
    zoneId: ZoneId = ZoneOffset.UTC,
): TemporalInterval<T> where T: Temporal, T: Comparable<T> =
    temporalIntervalOf((endExclusive - duration) as T, endExclusive, zoneId)


//
// MutableTemporalInterval
//

/**
 * [start]와 [endExclusive]를 이용하여 [MutableTemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = mutableTemporalIntervalOf(
 *   LocalDate.of(2021, 1, 1),
 *   LocalDate.of(2021, 1, 10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param start 시작 시각
 * @param endExclusive   완료 시각
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 * @return [MutableTemporalInterval] 인스턴스
 */
fun <T> mutableTemporalIntervalOf(
    start: T,
    endExclusive: T,
    zoneId: ZoneId = ZoneOffset.UTC,
): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> {
    return when {
        start < endExclusive -> MutableTemporalInterval(start, endExclusive, zoneId)
        else                 -> MutableTemporalInterval(endExclusive, start, zoneId)
    }
}

/**
 * [start]와 [duration]을 이용하여 [MutableTemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = mutableTemporalIntervalOf(
 *      LocalDate.of(2021, 1, 1),
 *      Period.ofDays(10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param start 시작 시각
 * @param duration 기간을 나타내는 정보
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 * @return [MutableTemporalInterval] 인스턴스
 */
@Suppress("UNCHECKED_CAST")
fun <T> mutableTemporalIntervalOf(
    start: T,
    duration: TemporalAmount,
    zoneId: ZoneId = ZoneOffset.UTC,
): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> =
    mutableTemporalIntervalOf(start, (start + duration) as T, zoneId)

/**
 * [duration]과 [endExclusive]를 이용하여 [MutableTemporalInterval] 인스턴스를 빌드합니다.
 *
 * ```
 * val interval = mutableTemporalIntervalOf(
 *    Period.ofDays(10),
 *    LocalDate.of(2021, 1, 10)
 * )
 * ```
 *
 * @param T [Temporal]의 하위 수형
 * @param duration 기간을 나타내는 정보
 * @param endExclusive      완료 시각
 * @param zoneId [ZoneId] (기본 값은 [ZoneOffset.UTC])
 * @return [MutableTemporalInterval] 인스턴스
 */
@Suppress("UNCHECKED_CAST")
fun <T> mutableTemporalIntervalOf(
    duration: TemporalAmount,
    endExclusive: T,
    zoneId: ZoneId = ZoneOffset.UTC,
): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> =
    mutableTemporalIntervalOf((endExclusive - duration) as T, endExclusive, zoneId)

//
// Conversions
//

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 [Duration]을 빌드합니다.
 */
fun <T> ReadableTemporalInterval<T>.toDuration(): Duration where T: Temporal, T: Comparable<T> =
    Duration.between(startInclusive, endExclusive)

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 Milliseconds 로 반환합니다.
 */
fun <T> ReadableTemporalInterval<T>.toDurationMillis(): Long where T: Temporal, T: Comparable<T> =
    toDuration().toMillis()

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 [TemporalInterval]를 빌드합니다.
 */
fun <T> ReadableTemporalInterval<T>.toInterval(): TemporalInterval<T> where T: Temporal, T: Comparable<T> {
    return temporalIntervalOf(startInclusive, endExclusive, zoneId)
}

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 [MutableTemporalInterval]를 빌드합니다.
 */
fun <T> ReadableTemporalInterval<T>.toMutableInterval(): MutableTemporalInterval<T> where T: Temporal, T: Comparable<T> {
    return mutableTemporalIntervalOf(startInclusive, endExclusive, zoneId)
}

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 [Period]을 빌드합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *    LocalDate.of(2021, 1, 1),
 *    LocalDate.of(2021, 1, 10)
 * interval.toPeriod() // Period.ofDays(9)
 * ```
 */
fun <T> ReadableTemporalInterval<T>.toPeriod(): Period where T: Temporal, T: Comparable<T> =
    Period.between(LocalDate.ofEpochDay(startInclusive.toEpochDay()), LocalDate.ofEpochDay(endExclusive.toEpochDay()))

/**
 * [ReadableTemporalInterval]의 시작시각과 완료시각으로 [unit] 단위의 [Period]을 빌드합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *      LocalDate.of(2021, 1, 1),
 *      LocalDate.of(2021, 1, 10)
 * )
 * interval.toPeriod(ChronoUnit.DAYS) // Period.ofDays(9)
 * ```
 *
 * @param unit [ChronoUnit] 단위
 * @return [Period] 인스턴스
 */
fun <T> ReadableTemporalInterval<T>.toPeriod(unit: ChronoUnit): Period where T: Temporal, T: Comparable<T> {
    return when (unit) {
        ChronoUnit.DAYS   -> Period.ofDays(toPeriod().days)
        ChronoUnit.WEEKS  -> Period.ofWeeks(toPeriod().days / 7)
        ChronoUnit.MONTHS -> Period.ofDays(toPeriod().months)
        ChronoUnit.YEARS  -> Period.ofDays(toPeriod().years)
        else              -> toPeriod()
    }
}


//
// Sequence of Interval
//

/**
 * [ReadableTemporalInterval]의 기간을 시간 단위 [unit]의 [step] 단계별로 열거합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *      LocalDate.of(2021, 1, 1),
 *      LocalDate.of(2021, 1, 10)
 * )
 * interval.sequence(1, ChronoUnit.DAYS).toList() // 일단위로 하루씩 증가하는 시퀀스 (2021-01-01, 2021-01-02 .. 2021-01-09)
 * ```
 *
 * @param step 증가 단계
 * @param unit [ChronoUnit] 단위
 */
@Suppress("UNCHECKED_CAST")
fun <T> ReadableTemporalInterval<T>.sequence(
    step: Int,
    unit: ChronoUnit,
): Sequence<T> where T: Temporal, T: Comparable<T> {
    step.assertPositiveNumber("step")

    return sequence {
        var current = startInclusive.startOf(unit)
        val increment = step.temporalAmount(unit)

        // TemporalInterval 은 OpenedRange ( [start, end) ) 입니다.
        while (current < endExclusive) {
            yield(current)
            current = (current + increment) as T
        }
    }

//    return object: Sequence<T> {
//        override fun iterator(): Iterator<T> {
//            var current = startInclusive.startOf(unit)
//            val increment = step.temporalAmount(unit)
//
//            // TemporalInterval 은 OpenedRange([start, end)) 입니다.
//            return object: Iterator<T> {
//                override fun hasNext(): Boolean = current < endExclusive
//
//                override fun next(): T {
//                    if (!hasNext()) throw NoSuchElementException("No more elements in the interval")
//                    val nextValue = current
//                    current = (current + increment) as T
//                    return nextValue
//                }
//            }
//        }
//    }
}

/**
 * [ReadableTemporalInterval]의 기간을 시간 단위 [unit]의 [step] 단계별로 emit 하는 Flow를 반환합니다.
 *
 * ```
 * val interval = temporalIntervalOf(
 *      LocalDate.of(2021, 1, 1),
 *      LocalDate.of(2021, 1, 10)
 * )
 * interval.flow(1, ChronoUnit.DAYS).toList() // 일단위로 하루씩 증가하는 시퀀스 (2021-01-01, 2021-01-02 .. 2021-01-09)
 * ```
 *
 * @param step 증가 단계
 * @param unit [ChronoUnit] 단위
 */
@Suppress("UNCHECKED_CAST")
fun <T> ReadableTemporalInterval<T>.flow(
    step: Int,
    unit: ChronoUnit,
): Flow<T> where T: Temporal, T: Comparable<T> {
    step.assertPositiveNumber("step")

    return flow {
        var current = startInclusive.startOf(unit)
        val increment = step.temporalAmount(unit)

        // TemporalInterval 은 OpenedRange ( [start, end) ) 입니다.
        while (current < endExclusive) {
            emit(current)
            current = (current + increment) as T
        }
    }
}

/**
 * 기간을 [ChronoUnit.MILLIS] 의 [step] 단계별로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.millis(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.MILLIS)
}

/**
 * 기간을 [ChronoUnit.SECONDS] 의 [step] 단계별로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.seconds(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.SECONDS)
}

/**
 * 기간을 [ChronoUnit.MINUTES] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.minutes(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.MINUTES)
}

/**
 * 기간을 [ChronoUnit.HOURS] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.hours(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.HOURS)
}

/**
 * 기간을 [ChronoUnit.DAYS] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.days(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.DAYS)
}

/**
 * 기간을 [ChronoUnit.WEEKS] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.weeks(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.WEEKS)
}

/**
 * 기간을 [ChronoUnit.MONTHS] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.months(months: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(months, ChronoUnit.MONTHS)
}

/**
 * 기간을 [ChronoUnit.YEARS] 단위로 열거합니다.
 */
fun <T> ReadableTemporalInterval<T>.years(step: Int = 1): Sequence<T> where T: Temporal, T: Comparable<T> {
    return sequence(step, ChronoUnit.YEARS)
}
