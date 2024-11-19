package io.bluetape4k.javatimes.period

import io.bluetape4k.ValueObject
import java.time.temporal.TemporalAccessor
import java.time.temporal.WeekFields

/**
 * Weekyear and Week
 */
data class WeekyearWeek(
    val weekyear: Int,
    val weekOfWeekyear: Int,
): ValueObject {

    companion object {

        @JvmStatic
        operator fun invoke(moment: TemporalAccessor): WeekyearWeek {
            val weekyear = moment[WeekFields.ISO.weekBasedYear()]
            val weekOfWeekyear = moment[WeekFields.ISO.weekOfWeekBasedYear()]
            return WeekyearWeek(weekyear, weekOfWeekyear)
        }
    }
}
