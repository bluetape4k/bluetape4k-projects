package io.bluetape4k.javatimes

import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.time.temporal.Temporal

operator fun Duration.unaryMinus(): Duration = this.negated()

/**
 * Duration 이 Positive 가 아닌지 여부 (0보다 작거나 같으면 true)
 */
val Duration.isNotPositive: Boolean get() = this <= Duration.ZERO

/**
 * Duration 이 Negative 가 아닌지 여부 (0보다 크거나 같으면 true)
 */
val Duration.isNotNegative: Boolean get() = this >= Duration.ZERO

/**
 * Duration을 millseconds 로 환산
 */
fun Duration.inMillis(): Long = toMillis()

/**
 * Duration을 nano seconds로 환산
 */
fun Duration.inNanos(): Long = toNanos() // seconds * NANO_PER_SECOND + nano

/**
 * [startInclusive] ~ [endExclusive] 의 기간을 [Duration]으로 빌드합니다.
 *
 * ```
 * val duration = durationOf(Instant.now(), Instant.now().plusSeconds(10))  // 10초간의 duration
 * ```
 *
 * @param startInclusive 시작 시각
 * @param endExclusive 끝 시각
 * @return Duration
 */
fun durationOf(startInclusive: Temporal, endExclusive: Temporal): Duration =
    Duration.between(startInclusive, endExclusive)

/**
 * [year]의 1년 단위의 기간
 *
 * ```kotlin
 * val duration = durationOfYear(2020)  // 2020.01.01 ~ 2020.12.31
 * ```
 * @param year Int
 * @return Duration
 */
fun durationOfYear(year: Int): Duration =
    durationOf(zonedDateTimeOf(year), zonedDateTimeOf(year + 1))

/**
 * [year]의 해당 [quarter]의 기간
 *
 * ```
 * val duration = durationOfQuarter(2020, Quarter.First)  // 2020.01.01 ~ 2020.03.31
 * ```
 *
 * @param year Int
 * @param quarter Quarter
 * @return Duration
 */
fun durationOfQuarter(year: Int, quarter: Quarter): Duration {
    val startInclusive = startOfQuarter(year, quarter)
    val endExclusive = startInclusive.plusMonths(MonthsPerQuarter.toLong())
    return durationOf(startInclusive, endExclusive)
}

/**
 * [year]의 해당 [monthOfYear]의 기간
 *
 * ```
 * val duration = durationOfMonth(2020, 1)  // 2020.01.01 ~ 2020.01.31
 * ```
 *
 * @param year Int
 * @param monthOfYear Int
 * @return Duration
 */
fun durationOfMonth(year: Int, monthOfYear: Int): Duration {
    val startInclusive = startOfMonth(year, monthOfYear)
    val endExclusive = startInclusive.plusMonths(1)
    return durationOf(startInclusive, endExclusive)
}

/**
 * [week] 수에 해당하는 기간
 *
 * ```
 * val duration = durationOfWeek(3)     // 3주간의 duration == (3 * 7).days()
 * ```
 * @param week Int
 * @return Duration
 */
fun durationOfWeek(week: Int): Duration =
    if (week == 0) Duration.ZERO else durationOfDay(week * DaysPerWeek)

/**
 * 지정한 시간 단위별 기간을 조합합니다.
 *
 * ```
 * val duration = durationOfDay(1, 2, 3, 4, 5)  // 1일 2시간 3분 4초 5나노초
 * ```
 *
 * @param days Int    일
 * @param hours Int   시간 (default: 0)
 * @param minutes Int 분 (default: 0)
 * @param seconds Int 초(default: 0)
 * @param nanos Int   나노초(default: 0)
 * @return Duration
 */
fun durationOfDay(
    days: Int,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0,
    nanos: Int = 0,
): Duration {
    var duration = days.days()

    if (hours != 0)
        duration += hours.hours()
    if (minutes != 0)
        duration += minutes.minutes()
    if (seconds != 0)
        duration += seconds.seconds()
    if (nanos != 0)
        duration += nanos.nanos()

    return duration
}

/**
 * 지정한 시간 단위별 기간을 조합합니다.
 *
 * ```
 * val duration = durationOfHour(1, 2, 3, 4)  // 1시간 2분 3초 4나노초
 * ```
 *
 * @param hours Int
 * @param minutes Int 분 (default: 0)
 * @param seconds Int 초(default: 0)
 * @param nanos Int   나노초(default: 0)
 * @return Duration
 */
fun durationOfHour(
    hours: Int,
    minutes: Int = 0,
    seconds: Int = 0,
    nanos: Int = 0,
): Duration {
    var duration = hours.hours()

    if (minutes != 0)
        duration += minutes.minutes()
    if (seconds != 0)
        duration += seconds.seconds()
    if (nanos != 0)
        duration += nanos.nanos()

    return duration
}

/**
 * 지정한 시간 단위별 기간을 조합합니다.
 *
 * ```
 * val duration = durationOfMinute(1, 2, 3)  // 1분 2초 3나노초
 * ```
 *
 * @param minutes Int 분
 * @param seconds Int 초(default: 0)
 * @param nanos Int   나노초(default: 0)
 * @return Duration
 */
fun durationOfMinute(
    minutes: Int,
    seconds: Int = 0,
    nanos: Int = 0,
): Duration {
    var duration = minutes.minutes()

    if (seconds != 0)
        duration += seconds.seconds()
    if (nanos != 0)
        duration += nanos.nanos()

    return duration
}

/**
 * 지정한 시간 단위별 기간을 조합합니다.
 *
 * ```
 * val duration = durationOfSecond(1, 2)  // 1초 2나노초
 * ```
 *
 * @param seconds Int 초
 * @param nanos Int   나노초(default: 0)
 * @return Duration
 */
fun durationOfSecond(
    seconds: Int,
    nanos: Int = 0,
): Duration {
    var duration = seconds.seconds()

    if (nanos != 0)
        duration += nanos.nanos()

    return duration
}

/**
 * [nanos] 수에 해당하는 Duration을 빌드합니다.
 *
 * ```
 * val duration = durationOfNano(1000)  // 1000 나노초
 * ```
 *
 * @param nanos nano seconds
 * @return Duration
 */
fun durationOfNano(nanos: Long): Duration = Duration.ofNanos(nanos)


/**
 * nano seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1.asNanos()  // 1나노초
 * ```
 */
fun Int.asNanos(): Duration = Duration.ofNanos(this.toLong())

/**
 * milli seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1.asMillis()  // 1밀리초
 * ```
 */
fun Int.asMillis(): Duration = Duration.ofMillis(this.toLong())

/**
 * seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1.asSeconds()  // 1초
 * ```
 */
fun Int.asSeconds(nanoAdjustment: Long = 0L): Duration = Duration.ofSeconds(this.toLong(), nanoAdjustment)

/**
 * minutes 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1.asMinutes()  // 1분
 * ```
 */
fun Int.asMinutes(): Duration = Duration.ofMinutes(this.toLong())

/**
 * hours 단위의 기간을 생성합니다.
 * ```
 * val duration = 1.asHours()  // 1시간
 * ```
 */
fun Int.asHours(): Duration = Duration.ofHours(this.toLong())

/**
 * days 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1.asDays()  // 1일
 * ```
 */
fun Int.asDays(): Duration = Duration.ofDays(this.toLong())

/**
 * nano seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asNanos()  // 1나노초
 * ```
 */
fun Long.asNanos(): Duration = Duration.ofNanos(this)

/**
 * milli seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asMillis()  // 1밀리초
 * ```
 */
fun Long.asMillis(): Duration = Duration.ofMillis(this)

/**
 * seconds 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asSeconds()  // 1초
 * ```
 */
fun Long.asSeconds(nanoAdjustment: Long = 0L): Duration = Duration.ofSeconds(this, nanoAdjustment)

/**
 * minutes 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asMinutes()  // 1분
 * ```
 */
fun Long.asMinutes(): Duration = Duration.ofMinutes(this)

/**
 * hours 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asHours()  // 1시간
 * ```
 */
fun Long.asHours(): Duration = Duration.ofHours(this)

/**
 * days 단위의 기간을 생성합니다.
 *
 * ```
 * val duration = 1L.asDays()  // 1일
 * ```
 */
fun Long.asDays(): Duration = Duration.ofDays(this)


/**
 * [Duration]을 ISO Format의 문자열로 만듭니다
 */
fun Duration.formatISO(): String = DurationFormatUtils.formatDurationISO(inMillis())

/**
 * [Duration]을 시간 포맷 (HH:mm:ss.SSS)의 문자열로 변환합니다.
 *
 * ```
 * val duration = 1.asHours() + 2.asMinutes() + 3.asSeconds() + 4.asMillis()
 * duration.formatHMS()  // "01:02:03.004"
 * ```
 *
 * @return 시간 포맷 (HH:mm:ss.SSS)의 문자열
 */
fun Duration.formatHMS(): String = DurationFormatUtils.formatDurationHMS(inMillis())

/**
 * [Duration]을 ISO Format의 문자열로 표현한 정규식
 */
private val durationIsoFormat: Regex =
    """P(?<year>\d+)Y(?<month>\d+)M(?<day>\d+)DT(?<hour>\d+)H(?<minute>\d+)M(?<second>\d+)\.(?<mills>\d{3})S""".toRegex()

/**
 * ISO Format으로 표현된 Duration 정보를 파싱해서 일, 시, 분, 초, 밀리초 만을 이용하여 Duration으로 변경한다.
 *
 * ```
 * val duration = parseIsoFormattedDuration("P1Y2M3DT4H5M6.007S")
 * println(duration)  // PT28H5M6.007S
 * ```
 *
 * @param isoFormattedString ISO Formatted Duration
 * @return [Duration] instance
 */
fun parseIsoFormattedDuration(isoFormattedString: String): Duration? {
    val matchResult = durationIsoFormat.matchEntire(isoFormattedString)
    return matchResult?.let {
        val (_, _, d, h, min, s, ms) = it.destructured
        Duration.ofDays(d.toLong())
            .plusHours(h.toLong())
            .plusMinutes(min.toLong())
            .plusSeconds(s.toLong())
            .plusMillis(ms.toLong())
    }
}
