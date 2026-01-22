package io.bluetape4k.javatimes.period


import io.bluetape4k.AbstractValueObject
import io.bluetape4k.utils.Local

/**
 * Period 관련 작업 시 사용할 정보를 담은 context
 */
open class PeriodContext: AbstractValueObject() {

    companion object {
        @JvmField
        val TIME_CALENDAR_KEY: String = PeriodContext::class.java.name + ".current"
    }

    object Current {
        var calendar: ITimeCalendar
            get() = Local.getOrPut(TIME_CALENDAR_KEY) { TimeCalendar.Default }!!
            set(value) {
                Local[TIME_CALENDAR_KEY] = value
            }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is PeriodContext
    }
}
