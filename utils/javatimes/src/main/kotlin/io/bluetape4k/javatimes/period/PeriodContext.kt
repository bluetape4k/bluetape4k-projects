package io.bluetape4k.javatimes.period

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf
import io.bluetape4k.utils.Local

/**
 * Period 관련 작업 시 사용할 정보를 담은 context
 *
 * ```kotlin
 * val context = PeriodContext()
 * val calendar = PeriodContext.Current.calendar // 현재 스레드에 설정된 TimeCalendar
 * PeriodContext.Current.calendar = TimeCalendar.EmptyOffset
 * ```
 */
open class PeriodContext: AbstractValueObject() {
    companion object {
        @JvmField
        val TIME_CALENDAR_KEY: String = PeriodContext::class.java.name + ".current"
    }

    object Current {
        var calendar: ITimeCalendar
            get() = Local.getOrPut(TIME_CALENDAR_KEY) { TimeCalendar.Default } ?: TimeCalendar.Default
            set(value) {
                Local[TIME_CALENDAR_KEY] = value
            }
    }

    override fun equalProperties(other: Any): Boolean = other is PeriodContext

    override fun hashCode(): Int = hashOf(PeriodContext::class.java)
}
