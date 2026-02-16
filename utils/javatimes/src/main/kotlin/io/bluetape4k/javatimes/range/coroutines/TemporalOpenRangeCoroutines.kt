package io.bluetape4k.javatimes.range.coroutines

import io.bluetape4k.javatimes.range.SupportChronoUnits
import io.bluetape4k.javatimes.range.TemporalOpenedProgression
import io.bluetape4k.javatimes.range.TemporalOpenedRange
import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.support.assertPositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount

fun <T> TemporalOpenedProgression<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { emit(it) }
}

fun <T> TemporalOpenedRange<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { emit(it) }
}

@Suppress("UNCHECKED_CAST")
fun <T> TemporalOpenedRange<T>.windowedFlow(
    size: Int,
    step: Int = 1,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Flow<List<T>> where T: Temporal, T: Comparable<T> {

    size.assertPositiveNumber("size")
    step.assertPositiveNumber("step")
    assert(unit in SupportChronoUnits) { "Not supported ChronoUnit. unit=$unit" }

    return flow {
        var current: T = start.startOf(unit)
        val increment: TemporalAmount = step.temporalAmount(unit)

        while (current < endExclusive) {
            var index = 0
            val item = generateSequence {
                (current + (index++).temporalAmount(unit)) as T
            }
                .takeWhile { it < endExclusive }
                .toList()

            emit(item)
            current = (current + increment) as T
        }
    }
}

fun <T> TemporalOpenedRange<T>.windowedFlowYears(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.YEARS)

fun <T> TemporalOpenedRange<T>.windowedFlowMonths(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MONTHS)

fun <T> TemporalOpenedRange<T>.windowedFlowWeeks(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.WEEKS)

fun <T> TemporalOpenedRange<T>.windowedFlowDays(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.DAYS)

fun <T> TemporalOpenedRange<T>.windowedFlowHours(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.HOURS)

fun <T> TemporalOpenedRange<T>.windowedFlowMinutes(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MINUTES)

fun <T> TemporalOpenedRange<T>.windowedFlowSeconds(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.SECONDS)

fun <T> TemporalOpenedRange<T>.windowedFlowMillis(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MILLIS)


fun <T> TemporalOpenedRange<T>.chunkedFlow(
    size: Int,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, size, unit)

fun <T> TemporalOpenedRange<T>.chunkedFlowYears(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.YEARS)

fun <T> TemporalOpenedRange<T>.chunkedFlowMonths(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MONTHS)

fun <T> TemporalOpenedRange<T>.chunkedFlowWeeks(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.WEEKS)

fun <T> TemporalOpenedRange<T>.chunkedFlowDays(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.DAYS)

fun <T> TemporalOpenedRange<T>.chunkedFlowHours(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.HOURS)

fun <T> TemporalOpenedRange<T>.chunkedFlowMinutes(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MINUTES)

fun <T> TemporalOpenedRange<T>.chunkedFlowSeconds(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.SECONDS)

fun <T> TemporalOpenedRange<T>.chunkedFlowMillis(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MILLIS)


@Suppress("UNCHECKED_CAST")
fun <T> TemporalOpenedRange<T>.zipWithNextFlow(unit: ChronoUnit): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> {
    assert(unit in SupportChronoUnits) { "Not supoorted ChronoUnit. unit=$unit" }

    return flow {
        var current: T = start.startOf(unit)
        val increment: TemporalAmount = 1.temporalAmount(unit)
        val limit: T = (endExclusive - increment) as T

        while (current < limit) {
            val next: T = (current + increment) as T
            emit(current to next)
            current = next
        }
    }
}

fun <T> TemporalOpenedRange<T>.zipWithNextFlowYears(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.YEARS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowMonths(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MONTHS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowWeeks(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.WEEKS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowDays(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.DAYS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowHours(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.HOURS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowMinutes(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MINUTES)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowSeconds(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.SECONDS)

fun <T> TemporalOpenedRange<T>.zipWithNextFlowMillis(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MILLIS)
