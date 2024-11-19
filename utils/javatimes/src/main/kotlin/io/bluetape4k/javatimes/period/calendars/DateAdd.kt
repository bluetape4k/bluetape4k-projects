package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.MaxDuration
import io.bluetape4k.javatimes.isNotNegative
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.SeekBoundaryMode
import io.bluetape4k.javatimes.period.SeekDirection
import io.bluetape4k.javatimes.period.TimePeriodCollection
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.hasInsideWith
import io.bluetape4k.javatimes.period.timelines.TimeGapCalculator
import io.bluetape4k.javatimes.period.timelines.TimePeriodCombiner
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.time.Duration
import java.time.ZonedDateTime

/**
 * DateAdd
 */
open class DateAdd protected constructor() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(): DateAdd = DateAdd()
    }

    open var includePeriods: TimePeriodCollection = TimePeriodCollection()
        protected set

    open var excludePeriods: TimePeriodCollection = TimePeriodCollection()
        protected set

    /**
     * 시작 일자로부터 offset 기간이 지난 일자를 계산합니다. (기간에 포함될 기간과 제외할 기간을 명시적으로 지정해 놓을 수 있습니다.)
     *
     * ```
     * val start = zonedDateTimeOf(2011, 4, 12)
     * val period1 = TimeRange(
     *     zonedDateTimeOf(2011, 4, 20),
     *     zonedDateTimeOf(2011, 4, 25)
     * )
     * val period2 = TimeRange(
     *     zonedDateTimeOf(2011, 4, 30),
     *     null
     * ) // 4월 30일 이후
     *
     * // 예외기간을 설정합니다. (휴일, 휴가 등)
     * // 4월 20일 ~ 40월 25일, 4월 30일 이후
     * val dateAdd = DateAdd().apply {
     *     excludePeriods.add(period1)
     *     excludePeriods.add(period2)
     * }
     *
     * dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)
     *
     * // 4월 12일에 8일을 더하면 4월 20일이지만, 20~25일까지 제외되므로, 4월 25일이 된다.
     * dateAdd.add(start, 8.days()) shouldBeEqualTo period1.end
     *
     * // 4월 12일에 20일을 더하면 4월 20~25일을 제외한 후 계산하면 4월 30 이후가 된다. (5월 3일)
     * // 하지만 4월 30일 이후는 모두 제외되므로 결과값은 null 이다.
     * dateAdd.add(start, 20.days()).shouldBeNull()
     * dateAdd.subtract(start, 3.days()) shouldBeEqualTo start.minusDays(3)
     * ```
     *
     * @param start        시작 일자
     * @param offset       기간
     * @param seekBoundary 마지막 일자 포함 여부
     * @return 시작 일자로부터 offset 기간 이후의 일자
     */
    open fun add(
        start: ZonedDateTime,
        offset: Duration,
        seekBoundary: SeekBoundaryMode = SeekBoundaryMode.NEXT,
    ): ZonedDateTime? {
        log.trace { "Add start=$start, offset=$offset, seekBoundary=$seekBoundary" }

        // 예외 조건이 없으면 단순 계산으로 처리
        if (includePeriods.isEmpty() && excludePeriods.isEmpty()) {
            return start + offset
        }

        val (end, remaining) = when {
            offset.isNegative -> calculateEnd(start, offset.negated(), SeekDirection.BACKWARD, seekBoundary)
            else              -> calculateEnd(start, offset, SeekDirection.FORWARD, seekBoundary)
        }

        log.trace { "Add results. end=$end, remaining=$remaining" }
        return end
    }

    /**
     * 시작 일자로부터 offset 기간 이전의 일자를 계산합니다. (기간에 포함될 기간과 제외할 기간을 명시적으로 지정해 놓을 수 있습니다.)
     *
     * ```
     * val start = zonedDateTimeOf(2011, 4, 30)
     * val period1 = TimeRange(
     *     zonedDateTimeOf(2011, 4, 20),
     *     zonedDateTimeOf(2011, 4, 25)
     * )
     * val period2 = TimeRange(
     *     null,
     *     zonedDateTimeOf(2011, 4, 6)
     * ) // ~ 4월 6일까지
     *
     * val dateAdd = DateAdd().apply {
     *     excludePeriods.add(period1)
     *     excludePeriods.add(period2)
     * }
     * dateAdd.subtract(start, 1.days()) shouldBeEqualTo start.minusDays(1)
     *
     * // 4월 30일로부터 5일 전이면 4월 25일이지만, 예외기간이므로 4월 20일이 된다.
     * dateAdd.subtract(start, 5.days()) shouldBeEqualTo period1.start
     *
     * // 4월 30일로부터 20일 전이면 4월 10일이지만, 예외기간 때문에 4월 5일이 된다. 근데 4월 6일 이전은 모두 제외기간이므로 null 을 반환한다.
     * dateAdd.subtract(start, 20.days()).shouldBeNull()
     * ```
     *
     * @param start        시작 일자
     * @param offset       기간
     * @param seekBoundary 마지막 일자 포함 여부
     * @return 시작 일자로부터 offset 기간 이전의 일자
     */
    open fun subtract(
        start: ZonedDateTime,
        offset: Duration,
        seekBoundary: SeekBoundaryMode = SeekBoundaryMode.NEXT,
    ): ZonedDateTime? {
        log.trace { "Substract start=$start, offset=$offset, seekBoundary=$seekBoundary" }

        // 예외 조건이 없으면 단순 계산으로 처리
        if (includePeriods.isEmpty() && excludePeriods.isEmpty()) {
            return start - offset
        }

        val (end, remaining) = when {
            offset.isNegative -> calculateEnd(start, offset.negated(), SeekDirection.FORWARD, seekBoundary)
            else              -> calculateEnd(start, offset, SeekDirection.BACKWARD, seekBoundary)
        }

        log.trace { "Substract results. end=$end, remaining=$remaining" }
        return end
    }

    @JvmOverloads
    protected open fun calculateEnd(
        start: ZonedDateTime,
        offset: Duration?,
        seekDir: SeekDirection,
        seekBoundary: SeekBoundaryMode = SeekBoundaryMode.NEXT,
    ): Pair<ZonedDateTime?, Duration?> {
        check(offset?.isNotNegative ?: false) { "offset 값은 0 이상이어야 합니다." }
        log.trace { "calculateEnd start=$start, offset=$offset, seekDir=$seekDir, seekBoundary=$seekBoundary" }

        val searchPeriods = TimePeriodCollection.ofAll(includePeriods.periods)
        if (searchPeriods.isEmpty()) {
            searchPeriods += TimeRange.AnyTime
        }

        // available periods
        var availablePeriods = getAvailablePeriods(searchPeriods)
        if (availablePeriods.isEmpty()) {
            return Pair(null, offset)
        }

        val periodCombiner = TimePeriodCombiner<TimeRange>()
        availablePeriods = periodCombiner.combinePeriods(availablePeriods)

        val startPeriod = when {
            seekDir.isForward -> findNextPeriod(start, availablePeriods)
            else              -> findPrevPeriod(start, availablePeriods)
        }

        // 첫 시작 기간이 없다면 중단한다.
        if (startPeriod.first == null) {
            log.trace { "startPeriod.first is null" }
            return Pair(null, offset)
        }

        if (offset == Duration.ZERO) {
            log.trace { "offset is zero, return Pair(${startPeriod.second}, $offset)" }
            return Pair(startPeriod.second, offset)
        }

        log.trace { "startPeriod=$startPeriod, offset=$offset" }

        return when {
            seekDir.isForward -> findPeriodForward(
                availablePeriods,
                offset,
                startPeriod.first,
                startPeriod.second,
                seekBoundary
            )

            else              -> findPeriodBackward(
                availablePeriods,
                offset,
                startPeriod.first,
                startPeriod.second,
                seekBoundary
            )
        }
    }

    private fun getAvailablePeriods(searchPeriods: ITimePeriodCollection): ITimePeriodCollection {
        val availablePeriods = TimePeriodCollection()

        if (excludePeriods.isEmpty()) {
            availablePeriods.addAll(searchPeriods)
        } else {
            val gapCalculator = TimeGapCalculator<TimeRange>()

            searchPeriods
                .forEach { p ->
                    if (excludePeriods.hasOverlapPeriods(p)) {
                        gapCalculator.gaps(excludePeriods, p).forEach { gap -> availablePeriods += gap }
                    } else {
                        availablePeriods += p
                    }
                }
        }

        log.trace { "availablePeriods=$availablePeriods" }
        return availablePeriods
    }

    @Suppress("NAME_SHADOWING")
    private fun findPeriodForward(
        availablePeriods: ITimePeriodCollection,
        remaining: Duration?,
        startPeriod: ITimePeriod?,
        seekMoment: ZonedDateTime,
        seekBoundary: SeekBoundaryMode,
    ): Pair<ZonedDateTime?, Duration?> {
        log.trace { "find period forward remaining=$remaining" }

        var seekMoment = seekMoment
        var remaining = remaining

        val startIndex = availablePeriods.indexOf(startPeriod)
        val length = availablePeriods.size

        for (i in startIndex until length) {
            val gap = availablePeriods[i]
            val gapRemaining = Duration.between(seekMoment, gap.end)

            log.trace { "gap=$gap, gapRemaining=$gapRemaining, remaining=$remaining, seekMoment=$seekMoment" }

            val isTargetPeriod = when {
                seekBoundary.isFill -> gapRemaining >= remaining
                else                -> gapRemaining > remaining
            }

            if (isTargetPeriod) {
                val foundMoment = seekMoment + remaining
                log.trace { "find datetime=$foundMoment" }
                return Pair(foundMoment, null)
            }

            remaining = remaining?.minus(gapRemaining)

            if (i < length - 1) {
                seekMoment = availablePeriods[i + 1].start
            }
        }

        log.trace { "해당일자를 찾지 못했습니다. remaining=${remaining.toString()}" }
        return Pair(null, remaining)
    }

    @Suppress("NAME_SHADOWING")
    private fun findPeriodBackward(
        availablePeriods: ITimePeriodCollection,
        remaining: Duration?,
        startPeriod: ITimePeriod?,
        seekMoment: ZonedDateTime,
        seekBoundary: SeekBoundaryMode,
    ): Pair<ZonedDateTime?, Duration?> {
        log.trace { "find period backward remaining=$remaining" }

        var seekMoment = seekMoment
        var remaining = remaining

        val startIndex = availablePeriods.indexOf(startPeriod)
        // val length = availablePeriods.size

        (startIndex downTo 0)
            .forEach { i ->
                val gap = availablePeriods[i]
                val gapRemaining = Duration.between(gap.start, seekMoment)

                log.trace { "gap=$gap, gapRemaining=$gapRemaining, remaining=$remaining, seekMoment=$seekMoment" }

                val isTargetPeriod = when {
                    seekBoundary.isFill -> gapRemaining >= remaining
                    else                -> gapRemaining > remaining
                }

                if (isTargetPeriod) {
                    val foundMoment = seekMoment - remaining
                    log.trace { "find datetime=$foundMoment" }
                    return Pair(foundMoment, null)
                }

                remaining = remaining?.minus(gapRemaining)

                if (i > 0) {
                    seekMoment = availablePeriods[i - 1].end
                }
            }

        log.trace { "해당일자를 찾지 못했습니다. remaining=$remaining" }
        return Pair(null, remaining)
    }

    private fun findNextPeriod(
        start: ZonedDateTime,
        periods: Collection<ITimePeriod>,
    ): Pair<ITimePeriod?, ZonedDateTime> {
        var nearest: ITimePeriod? = null
        var moment = start
        var diff = MaxDuration

        log.trace { "find next period. start=$start, periods=$periods" }

        var pair: Pair<ITimePeriod?, ZonedDateTime>? = null
        periods
            .filter { it.end >= start }
            .firstOrNull { period ->
                // start가 기간에 속한다면
                if (period.hasInsideWith(start)) {
                    pair = Pair(period, start)
                    return@firstOrNull true
                }

                // 근처 값이 아니라면 포기
                val periodToMoment = Duration.between(start, period.end)
                log.trace { "diff=$diff, periodToMoment=$periodToMoment" }
                if (periodToMoment < diff) {
                    diff = periodToMoment
                    nearest = period
                    moment = period.start
                }
                false
            }
        return pair ?: Pair(nearest, moment)
    }

    private fun findPrevPeriod(
        start: ZonedDateTime,
        periods: Collection<ITimePeriod>,
    ): Pair<ITimePeriod?, ZonedDateTime> {
        var nearest: ITimePeriod? = null
        var moment = start
        var diff = MaxDuration

        log.trace { "find prev period. start=$start, periods=$periods" }

        var pair: Pair<ITimePeriod?, ZonedDateTime>? = null
        periods
            // .asFlow()
            .filter { it.start <= start }
            .firstOrNull { period ->
                // start가 기간에 속한다면
                if (period.hasInsideWith(start)) {
                    pair = Pair(period, start)
                    return@firstOrNull true
                }

                // 근처 값이 아니라면 포기
                val periodToMoment = Duration.between(start, period.end)
                if (periodToMoment < diff) {
                    diff = periodToMoment
                    nearest = period
                    moment = period.end
                }
                false
            }

        return pair ?: Pair(nearest, moment)
    }
}
