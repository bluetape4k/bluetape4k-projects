package io.bluetape4k.javatimes.period

import java.time.ZonedDateTime

/**
 * 시작시각 ~ 완료시각의 기간을 표현하는 최상위 Interface
 */
interface ITimeRange: ITimePeriod {

    override var start: ZonedDateTime
    override var end: ZonedDateTime

    /**
     * 시작시각을 지정된 시각으로 설정합니다. 시작시각 이전이여야 합니다.
     */
    fun expandStartTo(moment: ZonedDateTime)

    /**
     * 완료시각을 지정된 시각으로 설정합니다. 완료시각 이후여야 합니다.
     */
    fun expandEndTo(moment: ZonedDateTime)

    /**
     * 가능하다면 시작시각, 완료시각을 지정된 시각으로 설정합니다.
     */
    fun expandTo(moment: ZonedDateTime) {
        expandStartTo(moment)
        expandEndTo(moment)
    }

    /**
     * 시작시각과 완료시각을 지정된 기간으로 설정합니다.
     */
    fun expandTo(target: ITimePeriod) {
        if (target.hasStart) expandStartTo(target.start)
        if (target.hasEnd) expandEndTo(target.end)
    }

    /**
     * 시작시각을 지정된 시각으로 설정합니다. 시작시각 이후여야 합니다.
     */
    fun shrinkStartTo(moment: ZonedDateTime)

    /**
     * 완료시각을 지정된 시각으로 설정합니다. 완료시각 이전이어야 합니다.
     */
    fun shrinkEndTo(moment: ZonedDateTime)

    /**
     * 시작시각, 완료시각을 지정된 시각으로 설정합니다.
     */
    fun shrinkTo(moment: ZonedDateTime) {
        shrinkStartTo(moment)
        shrinkEndTo(moment)
    }

    /**
     * 시작시각과 완료시각을 지정된 기간으로 설정합니다.
     */
    fun shrinkTo(target: ITimePeriod) {
        if (target.hasStart) shrinkStartTo(target.start)
        if (target.hasEnd) shrinkEndTo(target.end)
    }
}
