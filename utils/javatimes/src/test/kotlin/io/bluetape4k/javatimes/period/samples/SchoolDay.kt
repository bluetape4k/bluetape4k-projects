package io.bluetape4k.javatimes.period.samples

import io.bluetape4k.javatimes.durationOfMinute
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.period.TimeBlock
import io.bluetape4k.javatimes.period.TimePeriodChain
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

class SchoolDay(
    moment: ZonedDateTime = todayZonedDateTime() + 8.hours(),
): TimePeriodChain() {

    val lesson1: Lesson
    val break1: ShortBreak
    val lesson2: Lesson
    val break2: LargeBreak
    val lesson3: Lesson
    val break3: ShortBreak
    val lesson4: Lesson

    init {
        var start = moment
        lesson1 = Lesson(start)

        start += lesson1.duration
        break1 = ShortBreak(start)

        start += break1.duration
        lesson2 = Lesson(start)

        start += lesson2.duration
        break2 = LargeBreak(start)

        start += break2.duration
        lesson3 = Lesson(start)

        start += lesson3.duration
        break3 = ShortBreak(start)

        start += break3.duration
        lesson4 = Lesson(start)

        super.addAll(listOf(lesson1, break1, lesson2, break2, lesson3, break3, lesson4))
    }

    companion object {
        @JvmField
        val LessonDuration = durationOfMinute(50)

        @JvmField
        val LargeBreakDuration = durationOfMinute(15)

        @JvmField
        val ShortBreakDuration = durationOfMinute(5)
    }
}

class Lesson(start: ZonedDateTime): TimeBlock(start, start + SchoolDay.LessonDuration)

class LargeBreak(start: ZonedDateTime): TimeBlock(start, start + SchoolDay.LargeBreakDuration)

class ShortBreak(start: ZonedDateTime): TimeBlock(start, start + SchoolDay.ShortBreakDuration)
