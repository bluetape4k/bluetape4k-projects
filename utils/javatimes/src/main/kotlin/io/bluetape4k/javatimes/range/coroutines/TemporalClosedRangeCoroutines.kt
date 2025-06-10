package io.bluetape4k.javatimes.range.coroutines

import io.bluetape4k.javatimes.range.SupportChronoUnits
import io.bluetape4k.javatimes.range.TemporalClosedProgression
import io.bluetape4k.javatimes.range.TemporalClosedRange
import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.support.assertPositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * [TemporalClosedProgression]를 Flow 로 변환합니다.
 */
fun <T> TemporalClosedProgression<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { value -> emit(value) }
}

/**
 * [TemporalClosedRange]를 Flow 로 변환합니다.
 */
fun <T> TemporalClosedRange<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { value -> emit(value) }
}

/**
 * [TemporalClosedRange]를 [size] 만큼의 크기로 이동하면서 window를 만들어서 emit 하는 [Flow]를 빌드합니다.
 */
@Suppress("UNCHECKED_CAST")
fun <T> TemporalClosedRange<T>.windowedFlow(
    size: Int,
    step: Int = 1,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Flow<List<T>>
        where T: Temporal, T: Comparable<T> {
    size.assertPositiveNumber("size")
    step.assertPositiveNumber("step")
    assert(unit in SupportChronoUnits) { "Not supoorted ChronoUnit. unit=$unit" }

    return flow {
        var current: T = start.startOf(unit)
        val increment = step.temporalAmount(unit)

        while (current <= endInclusive) {
            val item = List(size) { (current + it.temporalAmount(unit)) as T }.takeWhile { it <= endInclusive }
            emit(item)
            current = (current + increment) as T
        }
    }
}

fun <T> TemporalClosedRange<T>.windowedFlowYears(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.YEARS)

fun <T> TemporalClosedRange<T>.windowedFlowMonths(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MONTHS)

fun <T> TemporalClosedRange<T>.windowedFlowWeeks(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.WEEKS)

fun <T> TemporalClosedRange<T>.windowedFlowDays(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.DAYS)

fun <T> TemporalClosedRange<T>.windowedFlowHours(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.HOURS)

fun <T> TemporalClosedRange<T>.windowedFlowMinutes(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MINUTES)

fun <T> TemporalClosedRange<T>.windowedFlowSeconds(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.SECONDS)

fun <T> TemporalClosedRange<T>.windowedFlowMillis(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MILLIS)

fun <T> TemporalClosedRange<T>.chunkedFlow(
    size: Int,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, size, unit)

fun <T> TemporalClosedRange<T>.chunkedFlowYears(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.YEARS)


fun <T> TemporalClosedRange<T>.chunkedFlowMonths(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MONTHS)


fun <T> TemporalClosedRange<T>.chunkedFlowWeeks(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.WEEKS)


fun <T> TemporalClosedRange<T>.chunkedFlowDays(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.DAYS)


fun <T> TemporalClosedRange<T>.chunkedFlowHours(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.HOURS)


fun <T> TemporalClosedRange<T>.chunkedFlowMinutes(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MINUTES)


fun <T> TemporalClosedRange<T>.chunkedFlowSeconds(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.SECONDS)


fun <T> TemporalClosedRange<T>.chunkedFlowMillis(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MILLIS)


@Suppress("UNCHECKED_CAST")
fun <T> TemporalClosedRange<T>.zipWithNextFlow(unit: ChronoUnit): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> {
    assert(unit in SupportChronoUnits) { "Not supoorted ChronoUnit. unit=$unit" }

    return flow {
        var current: T = start.startOf(unit)
        val increment = 1.temporalAmount(unit)
        val limit: T = (endInclusive - increment) as T

        while (current <= limit) {
            val next = (current + increment) as T
            emit(current to next)
            current = next
        }
    }
}

fun <T> TemporalClosedRange<T>.zipWithNextFlowYears(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.YEARS)

fun <T> TemporalClosedRange<T>.zipWithNextFlowMonths(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MONTHS)

fun <T> TemporalClosedRange<T>.zipWithNextFlowDays(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.DAYS)

fun <T> TemporalClosedRange<T>.zipWithNextFlowHours(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.HOURS)

fun <T> TemporalClosedRange<T>.zipWithNextFlowMinutes(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MINUTES)

fun <T> TemporalClosedRange<T>.zipWithNextFlowSeconds(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.SECONDS)

fun <T> TemporalClosedRange<T>.zipWithNextFlowMillis(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MILLIS)
