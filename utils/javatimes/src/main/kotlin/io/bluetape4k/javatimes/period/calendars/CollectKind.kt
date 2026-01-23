package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.support.requireInRange

/**
 * [CalendarPeriodCollector]가 수집할 방식
 */
enum class CollectKind {

    Year, Month, Day, Hour, Minute;

    val value: Int = ordinal

    companion object {

        @JvmStatic
        fun of(value: Int): CollectKind {
            value.requireInRange(0, 4, "value")
            return entries[value]
        }
    }
}
