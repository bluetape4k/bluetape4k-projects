package io.bluetape4k.javatimes.period.samples

import io.bluetape4k.javatimes.isNotNegative
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimeRange
import io.bluetape4k.javatimes.period.TimeRange
import java.time.Duration
import java.time.ZonedDateTime

class TimeRangePeriodRelationTestData(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val duration: Duration,
) {

    val allPeriods = mutableListOf<ITimePeriod>()

    var reference: ITimeRange
    var before: ITimeRange
    var startTouching: ITimeRange
    var startInside: ITimeRange
    var insideStartTouching: ITimeRange
    var enclosingStartTouching: ITimeRange
    var inside: ITimeRange
    var enclosingEndTouching: ITimeRange
    var exactMatch: ITimeRange
    var enclosing: ITimeRange
    var insideEndTouching: ITimeRange
    var endInside: ITimeRange
    var endTouching: ITimeRange
    var after: ITimeRange

    init {
        check(duration.isNotNegative) { "duration은 0 이상의 값을 가져야 합니다." }

        reference = TimeRange(start, end, true)

        val beforeEnd = start - duration
        val beforeStart = beforeEnd - reference.duration
        val insideStart = start + duration
        val insideEnd = end - duration
        val afterStart = end + duration
        val afterEnd = afterStart + reference.duration

        after = TimeRange(beforeStart, beforeEnd, true)
        startTouching = TimeRange(beforeStart, start, true)
        startInside = TimeRange(beforeStart, insideStart, true)
        insideStartTouching = TimeRange(start, afterStart, true)
        enclosingStartTouching = TimeRange(start, insideEnd, true)
        enclosing = TimeRange(insideStart, insideEnd, true)
        enclosingEndTouching = TimeRange(insideStart, end, true)
        exactMatch = TimeRange(start, end, true)
        inside = TimeRange(beforeStart, afterEnd, true)
        insideEndTouching = TimeRange(beforeStart, end, true)
        endInside = TimeRange(insideEnd, afterEnd, true)
        endTouching = TimeRange(end, afterEnd, true)
        before = TimeRange(afterStart, afterEnd, true)

        allPeriods.addAll(
            listOf(
                reference,
                after,
                startTouching,
                insideStartTouching,
                enclosingStartTouching,
                enclosing,
                enclosingEndTouching,
                exactMatch,
                inside,
                insideEndTouching,
                endInside,
                endTouching,
                before
            )
        )
    }
}
