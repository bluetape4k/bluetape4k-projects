package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.support.requireInRange

/**
 * [CalendarPeriodCollector]가 수집할 방식
 *
 * ```kotlin
 * val kind = CollectKind.Day      // 일 단위 수집
 * val byValue = CollectKind.of(2) // CollectKind.Day (ordinal=2)
 * kind.value // 2
 * ```
 */
enum class CollectKind {

    Year, Month, Day, Hour, Minute;

    val value: Int = ordinal

    companion object {

        /**
         * ordinal 값으로부터 [CollectKind]를 반환합니다.
         *
         * ```kotlin
         * CollectKind.of(0) // Year
         * CollectKind.of(2) // Day
         * CollectKind.of(4) // Minute
         * ```
         */
        @JvmStatic
        fun of(value: Int): CollectKind {
            value.requireInRange(0, 4, "value")
            return entries[value]
        }
    }
}
