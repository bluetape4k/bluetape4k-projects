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

/**
 * [TemporalOpenedProgression]을 [Flow]로 변환합니다.
 */
fun <T> TemporalOpenedProgression<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { emit(it) }
}

/**
 * [TemporalOpenedRange]를 [Flow]로 변환합니다.
 */
fun <T> TemporalOpenedRange<T>.asFlow(): Flow<T> where T: Temporal, T: Comparable<T> = flow {
    sequence().forEach { emit(it) }
}

/**
 * [TemporalOpenedRange]를 지정한 크기와 단계로 윈도우 처리하여 [Flow]로 반환합니다.
 *
 * @param size 윈도우 크기
 * @param step 윈도우 이동 단계 (기본값: 1)
 * @param unit 시간 단위 (기본값: YEARS)
 * @return 윈도우 처리된 리스트의 [Flow]
 */
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

/**
 * [TemporalOpenedRange]를 연도 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowYears(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.YEARS)

/**
 * [TemporalOpenedRange]를 월 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowMonths(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MONTHS)

/**
 * [TemporalOpenedRange]를 주 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowWeeks(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.WEEKS)

/**
 * [TemporalOpenedRange]를 일 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowDays(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.DAYS)

/**
 * [TemporalOpenedRange]를 시간 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowHours(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.HOURS)

/**
 * [TemporalOpenedRange]를 분 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowMinutes(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MINUTES)

/**
 * [TemporalOpenedRange]를 초 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowSeconds(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.SECONDS)

/**
 * [TemporalOpenedRange]를 밀리초 단위로 윈도우 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.windowedFlowMillis(
    size: Int,
    step: Int = 1,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, step, ChronoUnit.MILLIS)


/**
 * [TemporalOpenedRange]를 지정한 크기로 청크 처리하여 [Flow]로 반환합니다.
 *
 * @param size 청크 크기
 * @param unit 시간 단위 (기본값: YEARS)
 * @return 청크 처리된 리스트의 [Flow]
 */
fun <T> TemporalOpenedRange<T>.chunkedFlow(
    size: Int,
    unit: ChronoUnit = ChronoUnit.YEARS,
): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    windowedFlow(size, size, unit)

/**
 * [TemporalOpenedRange]를 연도 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowYears(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.YEARS)

/**
 * [TemporalOpenedRange]를 월 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowMonths(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MONTHS)

/**
 * [TemporalOpenedRange]를 주 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowWeeks(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.WEEKS)

/**
 * [TemporalOpenedRange]를 일 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowDays(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.DAYS)

/**
 * [TemporalOpenedRange]를 시간 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowHours(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.HOURS)

/**
 * [TemporalOpenedRange]를 분 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowMinutes(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MINUTES)

/**
 * [TemporalOpenedRange]를 초 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowSeconds(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.SECONDS)

/**
 * [TemporalOpenedRange]를 밀리초 단위로 청크 처리하여 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.chunkedFlowMillis(size: Int): Flow<List<T>> where T: Temporal, T: Comparable<T> =
    chunkedFlow(size, ChronoUnit.MILLIS)


/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 쌍으로 묶어 [Flow]로 반환합니다.
 *
 * @param unit 시간 단위
 * @return 연속된 요소 쌍의 [Flow]
 */
@Suppress("UNCHECKED_CAST")
fun <T> TemporalOpenedRange<T>.zipWithNextFlow(unit: ChronoUnit): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> {
    assert(unit in SupportChronoUnits) { "Not supported ChronoUnit. unit=$unit" }

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

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 연도 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowYears(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.YEARS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 월 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowMonths(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MONTHS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 주 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowWeeks(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.WEEKS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 일 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowDays(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.DAYS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 시간 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowHours(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.HOURS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 분 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowMinutes(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MINUTES)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 초 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowSeconds(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.SECONDS)

/**
 * [TemporalOpenedRange]의 각 요소와 다음 요소를 밀리초 단위로 쌍으로 묶어 [Flow]로 반환합니다.
 */
fun <T> TemporalOpenedRange<T>.zipWithNextFlowMillis(): Flow<Pair<T, T>> where T: Temporal, T: Comparable<T> =
    zipWithNextFlow(ChronoUnit.MILLIS)
