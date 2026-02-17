package io.bluetape4k.javatimes.period.calendars.seekers

import io.bluetape4k.javatimes.period.calendars.ICalendarVisitorContext
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.logging.KLogging
import kotlin.math.absoluteValue

/**
 * 일(Day) 탐색 컨텍스트
 *
 * 특정 일수만큼 이동하여 해당 일을 찾는 컨텍스트
 *
 * @param startDay 시작 일
 * @param dayCount 탐색할 일수 (절댓값 사용)
 */
open class DaySeekerContext private constructor(
    val startDay: DayRange,
    dayCount: Int = 0,
): ICalendarVisitorContext {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(startDay: DayRange, dayCount: Int = 0): DaySeekerContext {
            return DaySeekerContext(startDay, dayCount.absoluteValue)
        }
    }

    /** 남은 일수 */
    var remainingDays: Int = dayCount

    /** 찾은 일 */
    var foundDay: DayRange? = null

    /** 탐색 완료 여부 */
    val isFinished: Boolean get() = remainingDays == 0

    /** 탐색 미완료 여부 */
    val notFinished: Boolean get() = remainingDays != 0

    /**
     * 일을 처리하고 남은 일수를 감소시킵니다.
     *
     * @param day 처리할 일
     */
    fun processDay(day: DayRange) {
        if (!isFinished) {
            --remainingDays

            if (isFinished) {
                foundDay = day
            }
        }
    }
}
