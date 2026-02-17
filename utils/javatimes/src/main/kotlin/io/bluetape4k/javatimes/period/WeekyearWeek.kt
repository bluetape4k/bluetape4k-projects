package io.bluetape4k.javatimes.period

import io.bluetape4k.ValueObject
import java.time.temporal.TemporalAccessor
import java.time.temporal.WeekFields

/**
 * 주차 연도(weekyear)와 주 번호를 나타내는 값 객체
 *
 * ISO-8601 주차 연도 체계를 사용하여 특정 주를 식별합니다.
 * 주차 연도는 일반 연도와 다를 수 있습니다. (예: 1월 1일이 속한 주가 이전 연도에 포함될 수 있음)
 *
 * @param weekyear 주차 연도
 * @param weekOfWeekyear 주차 연도 내의 주 번호
 */
data class WeekyearWeek(
    val weekyear: Int,
    val weekOfWeekyear: Int,
): ValueObject {

    companion object {

        /**
         * [TemporalAccessor]로부터 [WeekyearWeek]을 생성합니다.
         *
         * @param moment 시간 정보를 포함하는 [TemporalAccessor]
         * @return 생성된 [WeekyearWeek] 인스턴스
         */
        @JvmStatic
        operator fun invoke(moment: TemporalAccessor): WeekyearWeek {
            val weekyear = moment[WeekFields.ISO.weekBasedYear()]
            val weekOfWeekyear = moment[WeekFields.ISO.weekOfWeekBasedYear()]
            return WeekyearWeek(weekyear, weekOfWeekyear)
        }
    }
}
