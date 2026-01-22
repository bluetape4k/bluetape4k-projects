package io.bluetape4k.javatimes.interval

import io.bluetape4k.javatimes.isPositive
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount

/**
 * joda-time의 Interval을 참고하여 구현한 클래스입니다.
 *
 * 참고: [Interval.java](https://gist.github.com/simon04/26f68a3f21f76dc0bc1ff012676432c9)
 *
 * @property startInclusive 시작 시각
 * @property endExclusive 종료 시각 (제외)
 * @property zoneId ZoneId
 */
@Suppress("UNCHECKED_CAST")
class TemporalInterval<T> private constructor(
    override val startInclusive: T,
    override val endExclusive: T,
    override val zoneId: ZoneId,
): AbstractTemporalInterval<T>() where T: Temporal, T: Comparable<T> {

    companion object {
        @JvmStatic
        operator fun <T> invoke(
            start: T,
            endExclusive: T,
            zoneId: ZoneId = ZoneOffset.UTC,
        ): TemporalInterval<T> where T: Temporal, T: Comparable<T> {
            check(start < endExclusive) { "The end instant[$endExclusive] must be greater than the start instant[$start]." }
            return TemporalInterval(start, endExclusive, zoneId)
        }

        @JvmStatic
        operator fun <T> invoke(
            start: T,
            duration: TemporalAmount,
            zoneId: ZoneId = ZoneOffset.UTC,
        ): TemporalInterval<T> where T: Temporal, T: Comparable<T> {
            require(duration.isPositive) { "The duration must be positive." }
            return invoke(start, (start + duration) as T, zoneId)
        }

        @JvmStatic
        operator fun <T> invoke(
            duration: TemporalAmount,
            endExclusive: T,
            zoneId: ZoneId = ZoneOffset.UTC,
        ): TemporalInterval<T> where T: Temporal, T: Comparable<T> {
            return invoke((endExclusive - duration) as T, endExclusive, zoneId)
        }

        /**
         * [ZonedDateTime]로 표현한 기간을 parsing 합니다.
         *
         * ```
         * val interval = ZonedDateTimeInterval.parse("2021-01-01T00:00:00Z ~ 2021-01-02T00:00:00Z")
         *```
         *
         * @param str ISO-8601 형식의 문자열
         * @return TemporalInterval
         */
        fun parse(str: String): ZonedDateTimeInterval {
            val (leftStr, rightStr) = str.split(ReadableTemporalInterval.SEPARATOR, limit = 2)

            return temporalIntervalOf(
                ZonedDateTime.parse(leftStr.trim()),
                ZonedDateTime.parse(rightStr.trim())
            )
        }

        /**
         * [ZonedDateTime] 로 표현한 기간을 parsing 합니다.
         *
         * ```
         * val interval = ZonedDateTimeInterval.parseWithOffset("2021-01-01T00:00:00Z+09:00 ~ 2021-01-02T00:00:00Z+09:00")
         * ```
         *
         * @param str 파싱할 문자열 ([DateTimeFormatter.ISO_OFFSET_DATE_TIME] 형식)
         * @return TemporalInterval
         */
        fun parseWithOffset(str: CharSequence): ZonedDateTimeInterval {
            val (leftStr, rightStr) = str.split(ReadableTemporalInterval.SEPARATOR, limit = 2)

            return temporalIntervalOf(
                start = ZonedDateTime.parse(leftStr.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                endExclusive = ZonedDateTime.parse(rightStr.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )
        }

    }

    /**
     * `start`를 기준으로 `duration`을 가지는 [TemporalInterval]을 빌드한다
     * @param duration TemporalAmount
     * @return TemporalInterval
     */
    fun withAmountAfterStart(duration: TemporalAmount): TemporalInterval<T> =
        temporalIntervalOf(startInclusive, duration, zoneId)

    /**
     * End 를 기준으로 지정한 `duration`을 가지는 [TemporalInterval]을 빌드한다
     * @param duration TemporalAmount
     * @return TemporalInterval
     */
    fun withAmountBeforeEnd(duration: TemporalAmount): TemporalInterval<T> =
        temporalIntervalOf(duration, endExclusive, zoneId)
}
