package io.bluetape4k.javatimes.period

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.logging.KLogging
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 시간 단위의 달력을 나타내는 클래스
 *
 * @property config 달력 설정
 *
 * @see [ITimeCalendar]
 */
open class TimeCalendar(
    private val config: TimeCalendarConfig = TimeCalendarConfig.Default,
): AbstractValueObject(), ITimeCalendar {

    companion object: KLogging() {
        @JvmStatic
        val Default: TimeCalendar = TimeCalendar(TimeCalendarConfig.Default)

        @JvmStatic
        val EmptyOffset: TimeCalendar = TimeCalendar(TimeCalendarConfig.EmptyOffset)

        @JvmStatic
        fun invoke(cfg: TimeCalendarConfig = TimeCalendarConfig.Default): TimeCalendar = TimeCalendar(cfg)

        @JvmStatic
        fun of(cfg: TimeCalendarConfig = TimeCalendarConfig.Default): TimeCalendar = TimeCalendar(cfg)
    }

    override val startOffset: Duration get() = config.startOffset
    override val endOffset: Duration get() = config.endOffset
    override val firstDayOfWeek: DayOfWeek get() = config.firstDayOfWeek

    override fun mapStart(moment: ZonedDateTime): ZonedDateTime = when {
        moment > MinPeriodTime -> moment + startOffset
        else -> moment
    }

    override fun mapEnd(moment: ZonedDateTime): ZonedDateTime = when {
        moment < MaxPeriodTime -> moment + endOffset
        else -> moment
    }

    override fun unmapStart(moment: ZonedDateTime): ZonedDateTime = when {
        moment > MinPeriodTime -> moment - startOffset
        else -> moment
    }

    override fun unmapEnd(moment: ZonedDateTime): ZonedDateTime = when {
        moment < MaxPeriodTime -> moment - endOffset
        else -> moment
    }

    override fun equalProperties(other: Any): Boolean {
        return other is ITimeCalendar &&
                startOffset == other.startOffset &&
                endOffset == other.endOffset &&
                firstDayOfWeek == other.firstDayOfWeek
    }

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("startOffset", startOffset)
            .add("endOffset", endOffset)
            .add("firstDayOfWeek", firstDayOfWeek)
}
