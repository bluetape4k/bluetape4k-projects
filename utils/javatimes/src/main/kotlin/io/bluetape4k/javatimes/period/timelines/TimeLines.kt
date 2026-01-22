package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.TimePeriodCollection
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.logging.KLogging

/**
 * [ITimeLine]을 위한 유틸리티 클래스입니다.
 */
object TimeLines: KLogging() {

    fun combinePeriods(
        moments: ITimeLineMomentCollection,
    ): ITimePeriodCollection {
        if (moments.isEmpty()) {
            return TimePeriodCollection.EMPTY
        }

        val result = TimePeriodCollection()
        val momentsSize = moments.size
        var index = 0

        while (index < momentsSize) {
            val periodStart = moments[index]
            var balance = periodStart.startCount
            check(balance > 0) { "Balance must be positive number. balance=$balance" }

            var periodEnd: ITimeLineMoment? = null

            while (index < momentsSize - 1 && balance > 0) {
                index++
                periodEnd = moments[index]
                balance += periodEnd.startCount
                balance -= periodEnd.endCount
            }

            check(periodEnd != null) { "periodEnd must not be null." }
            if (periodEnd.startCount <= 0 && index < momentsSize) {
                result += TimeRange(periodStart.moment, periodEnd.moment)
            }
            index++
        }

        return result
    }

    fun intersectPeriods(
        moments: ITimeLineMomentCollection,
    ): ITimePeriodCollection {
        if (moments.isEmpty()) {
            return TimePeriodCollection.EMPTY
        }

        val result = TimePeriodCollection()
        val momentsSize = moments.size
        var intersectionStart = -1
        var balance = 0L
        var index = 0


        while (index < momentsSize) {
            val moment = moments[index]
            val startCount = moment.startCount
            val endCount = moment.endCount

            balance += startCount
            balance -= endCount

            if (startCount > 0 && balance > 1 && intersectionStart < 0) {
                intersectionStart = index
            } else if (endCount > 0 && balance <= 1 && intersectionStart >= 0) {
                result += TimeRange(moments[intersectionStart].moment, moment.moment)
                intersectionStart = -1
            }
            index++
        }
        return result
    }

    fun calculateGap(
        moments: ITimeLineMomentCollection,
        range: ITimePeriod,
    ): ITimePeriodCollection {
        if (moments.isEmpty()) {
            return TimePeriodCollection.EMPTY
        }

        val gaps = TimePeriodCollection()
        val periodStart: ITimeLineMoment? = moments.minOrNull()

        // 1. find leading gap
        periodStart?.let { start ->
            if (range.start < start.moment) {
                gaps += TimeRange(range.start, start.moment)
            }
        }

        // 2. find intermediated gap
        var index = 0
        while (index < moments.size) {
            val moment = moments[index]
            check(moment.startCount > 0) { "moment.startCount[${moment.startCount}] must be positive number." }

            var balance = moment.startCount
            var gapStart: ITimeLineMoment? = null

            while (index < moments.size - 1 && balance > 0) {
                index++
                gapStart = moments[index]

                balance += gapStart.startCount
                balance -= gapStart.endCount
            }

            check(gapStart != null) { "gapStart must not be null." }

            if (gapStart.startCount <= 0) {
                // found a gap
                if (index < moments.size - 1) {
                    gaps += TimeRange(gapStart.moment, moments[index + 1].moment)
                }
            }
            index++
        }

        // 3. find ending gap
        val periodEnd = moments.maxOrNull()
        periodEnd?.let { end ->
            if (range.end > end.moment) {
                gaps += TimeRange(end.moment, range.end)
            }
        }

        return gaps
    }
}
