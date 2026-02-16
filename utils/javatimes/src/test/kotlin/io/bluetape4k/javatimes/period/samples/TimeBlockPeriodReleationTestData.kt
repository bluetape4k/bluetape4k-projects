package io.bluetape4k.javatimes.period.samples

import io.bluetape4k.javatimes.isNotNegative
import io.bluetape4k.javatimes.period.ITimeBlock
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeBlock
import java.time.Duration
import java.time.ZonedDateTime

class TimeBlockPeriodRelationTestData(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val duration: Duration,
) {

    val allPeriods = mutableListOf<ITimePeriod>()

    var reference: ITimeBlock
    var before: ITimeBlock
    var startTouching: ITimeBlock
    var startInside: ITimeBlock
    var insideStartTouching: ITimeBlock
    var enclosingStartTouching: ITimeBlock
    var inside: ITimeBlock
    var enclosingEndTouching: ITimeBlock
    var exactMatch: ITimeBlock
    var enclosing: ITimeBlock
    var insideEndTouching: ITimeBlock
    var endInside: ITimeBlock
    var endTouching: ITimeBlock
    var after: ITimeBlock

    init {
        check(duration.isNotNegative) { "duration은 0 이상의 값을 가져야 합니다." }

        reference = TimeBlock(start, end, true)

        val beforeEnd = start - duration
        val beforeStart = beforeEnd - reference.duration
        val insideStart = start + duration
        val insideEnd = end - duration
        val afterStart = end + duration
        val afterEnd = afterStart + reference.duration

        after = TimeBlock(beforeStart, beforeEnd, true)
        startTouching = TimeBlock(beforeStart, start, true)
        startInside = TimeBlock(beforeStart, insideStart, true)
        insideStartTouching = TimeBlock(start, afterStart, true)
        enclosingStartTouching = TimeBlock(start, insideEnd, true)
        enclosing = TimeBlock(insideStart, insideEnd, true)
        enclosingEndTouching = TimeBlock(insideStart, end, true)
        exactMatch = TimeBlock(start, end, true)
        inside = TimeBlock(beforeStart, afterEnd, true)
        insideEndTouching = TimeBlock(beforeStart, end, true)
        endInside = TimeBlock(insideEnd, afterEnd, true)
        endTouching = TimeBlock(end, afterEnd, true)
        before = TimeBlock(afterStart, afterEnd, true)

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
