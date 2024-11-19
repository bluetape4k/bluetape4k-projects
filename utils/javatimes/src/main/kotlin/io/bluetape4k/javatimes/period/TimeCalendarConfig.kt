package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.DefaultEndOffset
import io.bluetape4k.javatimes.DefaultStartOffset
import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.FirstDayOfWeek
import java.io.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.util.*

/**
 * 기간 설정을 위한 설정 정보를 제공합니다.
 *
 * @param locale 기간, 시간, 날짜 등의 로케일 정보를 제공합니다. 기본값은 시스템 로케일입니다.
 * @param startOffset 기간의 시작점을 설정합니다. 기본값은 0 나노초입니다. (startInclusive 를 구현하기 위해)
 * @param endOffset 기간의 종료점을 설정합니다. 기본값은 -1 나노초입니다. (endExclusive 를 구현하기 위해)
 * @param firstDayOfWeek 주의 첫 요일을 설정합니다. 기본값은 ISO 8601 표준에 따라 [DayOfWeek.MONDAY] 입니다.
 */
data class TimeCalendarConfig(
    val locale: Locale = Locale.getDefault(),
    val startOffset: Duration = DefaultStartOffset,
    val endOffset: Duration = DefaultEndOffset,
    val firstDayOfWeek: DayOfWeek = FirstDayOfWeek,
): Serializable {

    companion object {
        /**
         * [TimeCalendarConfig]의 기본 정보
         */
        val Default: TimeCalendarConfig = TimeCalendarConfig()

        /**
         * Offset 설정이 모두 0 나노초로, start 와 end 모두 기간에 포함되도록 하는 설정입니다.
         */
        val EmptyOffset: TimeCalendarConfig =
            TimeCalendarConfig(startOffset = EmptyDuration, endOffset = EmptyDuration)
    }
}
