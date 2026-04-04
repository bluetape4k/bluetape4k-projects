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
 * ```kotlin
 * val date = LocalDate.of(2024, 1, 1)
 * val weekyearWeek = WeekyearWeek(date)
 * weekyearWeek.weekyear        // 2024 (또는 2023, ISO 기준에 따라)
 * weekyearWeek.weekOfWeekyear  // 주 번호
 * ```
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
            return invoke(moment, WeekFields.ISO)
        }

        /**
         * [TemporalAccessor]와 [WeekFields]로부터 [WeekyearWeek]을 생성합니다.
         *
         * ```kotlin
         * val date = LocalDate.of(2024, 1, 1)
         * val ww = WeekyearWeek(date, WeekFields.ISO)
         * ww.weekyear        // ISO 주차 연도
         * ww.weekOfWeekyear  // ISO 주 번호
         * ```
         */
        @JvmStatic
        operator fun invoke(moment: TemporalAccessor, weekFields: WeekFields): WeekyearWeek {
            val weekyear = moment[weekFields.weekBasedYear()]
            val weekOfWeekyear = moment[weekFields.weekOfWeekBasedYear()]
            return WeekyearWeek(weekyear, weekOfWeekyear)
        }
    }
}
