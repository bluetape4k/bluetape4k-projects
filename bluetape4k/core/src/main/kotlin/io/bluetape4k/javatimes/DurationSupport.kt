package io.bluetape4k.javatimes

import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.time.temporal.Temporal

/**
 * [Duration]의 부호를 반전합니다.
 *
 * ## 동작/계약
 * - `-duration` 형태로 사용할 수 있습니다.
 * - 반환 값은 새로운 [Duration]이며, 수신 객체는 변경되지 않습니다.
 *
 * ```kotlin
 * val d = 5.asSeconds()
 * val neg = -d
 * // neg == PT-5S
 * ```
 */
operator fun Duration.unaryMinus(): Duration = this.negated()

/**
 * [Duration]이 양수가 아닌지 여부를 반환합니다(0 이하이면 true).
 *
 * ## 동작/계약
 * - `this <= Duration.ZERO`와 동일합니다.
 * - 0은 양수가 아니므로 true입니다.
 *
 * ```kotlin
 * val a = 0.asSeconds().isNotPositive
 * val b = (-1).asSeconds().isNotPositive
 * val c = 1.asSeconds().isNotPositive
 * // a == true, b == true, c == false
 * ```
 */
val Duration.isNotPositive: Boolean get() = this <= Duration.ZERO

/**
 * [Duration]이 음수가 아닌지 여부를 반환합니다(0 이상이면 true).
 *
 * ## 동작/계약
 * - `this >= Duration.ZERO`와 동일합니다.
 * - 0은 음수가 아니므로 true입니다.
 *
 * ```kotlin
 * val a = 0.asSeconds().isNotNegative
 * val b = 1.asSeconds().isNotNegative
 * val c = (-1).asSeconds().isNotNegative
 * // a == true, b == true, c == false
 * ```
 */
val Duration.isNotNegative: Boolean get() = this >= Duration.ZERO

/**
 * [Duration]을 밀리초 단위 값으로 반환합니다.
 *
 * ## 동작/계약
 * - [Duration.toMillis]의 별칭입니다.
 * - 범위를 벗어나면 [ArithmeticException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = 1500.asMillis().inMillis()
 * // result == 1500
 * ```
 */
fun Duration.inMillis(): Long = toMillis()

/**
 * [Duration]을 나노초 단위 값으로 반환합니다.
 *
 * ## 동작/계약
 * - [Duration.toNanos]의 별칭입니다.
 * - 범위를 벗어나면 [ArithmeticException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = 10.asNanos().inNanos()
 * // result == 10
 * ```
 */
fun Duration.inNanos(): Long = toNanos() // seconds * NANO_PER_SECOND + nano

/**
 * 두 시각 [startInclusive] ~ [endExclusive] 사이의 기간을 [Duration]으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.between]을 사용합니다.
 * - 반환 값은 `endExclusive - startInclusive` 입니다.
 *
 * ```kotlin
 * val d = durationOf(zonedDateTimeOf(2020), zonedDateTimeOf(2020).plusSeconds(10))
 * // d.seconds == 10
 * ```
 */
fun durationOf(startInclusive: Temporal, endExclusive: Temporal): Duration =
    Duration.between(startInclusive, endExclusive)

/**
 * 지정한 [year]의 1년 기간을 [Duration]으로 반환합니다.
 *
 * ## 동작/계약
 * - 시작은 해당 연도의 1월 1일 0시(기본 타임존 기준)입니다.
 * - 끝은 다음 해 1월 1일 0시(배타)입니다.
 *
 * ```kotlin
 * val d = durationOfYear(2020)
 * // d.toDays() == 366 (윤년)
 * ```
 */
fun durationOfYear(year: Int): Duration =
    durationOf(zonedDateTimeOf(year), zonedDateTimeOf(year + 1))

/**
 * 지정한 [year]의 [quarter] 분기 기간을 [Duration]으로 반환합니다.
 *
 * ## 동작/계약
 * - 분기 시작 시각은 [startOfQuarter]로 계산합니다.
 * - 끝 시각은 시작 + 3개월(배타)입니다.
 *
 * ```kotlin
 * val d = durationOfQuarter(2020, Quarter.First)
 * // d.toDays() == 91
 * ```
 */
fun durationOfQuarter(year: Int, quarter: Quarter): Duration {
    val startInclusive = startOfQuarter(year, quarter)
    val endExclusive = startInclusive.plusMonths(MonthsPerQuarter.toLong())
    return durationOf(startInclusive, endExclusive)
}

/**
 * 지정한 [year]의 [monthOfYear]월 기간을 [Duration]으로 반환합니다.
 *
 * ## 동작/계약
 * - 월 시작 시각은 [startOfMonth]로 계산합니다.
 * - 끝 시각은 시작 + 1개월(배타)입니다.
 *
 * ```kotlin
 * val d = durationOfMonth(2020, 1)
 * // d.toDays() == 31
 * ```
 */
fun durationOfMonth(year: Int, monthOfYear: Int): Duration {
    val startInclusive = startOfMonth(year, monthOfYear)
    val endExclusive = startInclusive.plusMonths(1)
    return durationOf(startInclusive, endExclusive)
}

/**
 * [week] 주(week)에 해당하는 기간을 [Duration]으로 반환합니다.
 *
 * ## 동작/계약
 * - `week == 0`이면 [Duration.ZERO]를 반환합니다.
 * - 그 외에는 `week * 7일`을 기준으로 계산합니다.
 *
 * ```kotlin
 * val d = durationOfWeek(3)
 * // d.toDays() == 21
 * ```
 */
fun durationOfWeek(week: Int): Duration =
    if (week == 0) Duration.ZERO else durationOfDay(week * DaysPerWeek)

/**
 * 일/시/분/초/나노초 단위를 조합해 [Duration]을 생성합니다.
 *
 * ## 동작/계약
 * - 각 단위는 0이면 무시됩니다.
 * - 내부적으로 `days.days()` 및 `plus` 연산을 사용합니다.
 *
 * ```kotlin
 * val d = durationOfDay(days = 1, hours = 2, minutes = 3, seconds = 4, nanos = 5)
 * // d == PT26H3M4.000000005S
 * ```
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
 * 시/분/초/나노초 단위를 조합해 [Duration]을 생성합니다.
 *
 * ## 동작/계약
 * - 각 단위는 0이면 무시됩니다.
 * - 내부적으로 `hours.hours()` 및 `plus` 연산을 사용합니다.
 *
 * ```kotlin
 * val d = durationOfHour(hours = 1, minutes = 2, seconds = 3, nanos = 4)
 * // d == PT1H2M3.000000004S
 * ```
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
 * 분/초/나노초 단위를 조합해 [Duration]을 생성합니다.
 *
 * ## 동작/계약
 * - 각 단위는 0이면 무시됩니다.
 * - 내부적으로 `minutes.minutes()` 및 `plus` 연산을 사용합니다.
 *
 * ```kotlin
 * val d = durationOfMinute(minutes = 1, seconds = 2, nanos = 3)
 * // d == PT1M2.000000003S
 * ```
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
 * 초/나노초 단위를 조합해 [Duration]을 생성합니다.
 *
 * ## 동작/계약
 * - nanos가 0이면 초 단위만 생성합니다.
 * - 내부적으로 `seconds.seconds()` 및 `plus` 연산을 사용합니다.
 *
 * ```kotlin
 * val d = durationOfSecond(seconds = 1, nanos = 2)
 * // d == PT1.000000002S
 * ```
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
 * [nanos] 나노초에 해당하는 [Duration]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofNanos]를 사용합니다.
 *
 * ```kotlin
 * val d = durationOfNano(1000)
 * // d == PT0.000001S
 * ```
 */
fun durationOfNano(nanos: Long): Duration = Duration.ofNanos(nanos)


/**
 * nano seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofNanos]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asNanos()  // 1나노초
 * ```
 */
fun Int.asNanos(): Duration = Duration.ofNanos(this.toLong())

/**
 * milli seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofMillis]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asMillis()  // 1밀리초
 * ```
 */
fun Int.asMillis(): Duration = Duration.ofMillis(this.toLong())

/**
 * seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofSeconds]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asSeconds()  // 1초
 * ```
 */
fun Int.asSeconds(nanoAdjustment: Long = 0L): Duration = Duration.ofSeconds(this.toLong(), nanoAdjustment)

/**
 * minutes 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofMinutes]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asMinutes()  // 1분
 * ```
 */
fun Int.asMinutes(): Duration = Duration.ofMinutes(this.toLong())

/**
 * hours 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofHours]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asHours()  // 1시간
 * ```
 */
fun Int.asHours(): Duration = Duration.ofHours(this.toLong())

/**
 * days 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofDays]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1.asDays()  // 1일
 * ```
 */
fun Int.asDays(): Duration = Duration.ofDays(this.toLong())

/**
 * nano seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofNanos]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asNanos()  // 1나노초
 * ```
 */
fun Long.asNanos(): Duration = Duration.ofNanos(this)

/**
 * milli seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofMillis]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asMillis()  // 1밀리초
 * ```
 */
fun Long.asMillis(): Duration = Duration.ofMillis(this)

/**
 * seconds 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofSeconds]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asSeconds()  // 1초
 * ```
 */
fun Long.asSeconds(nanoAdjustment: Long = 0L): Duration = Duration.ofSeconds(this, nanoAdjustment)

/**
 * minutes 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofMinutes]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asMinutes()  // 1분
 * ```
 */
fun Long.asMinutes(): Duration = Duration.ofMinutes(this)

/**
 * hours 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofHours]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asHours()  // 1시간
 * ```
 */
fun Long.asHours(): Duration = Duration.ofHours(this)

/**
 * days 단위의 기간을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Duration.ofDays]를 사용합니다.
 *
 * ```kotlin
 * val duration = 1L.asDays()  // 1일
 * ```
 */
fun Long.asDays(): Duration = Duration.ofDays(this)


/**
 * [Duration]을 ISO-8601 형태의 문자열로 포맷팅합니다.
 *
 * ## 동작/계약
 * - 내부적으로 Apache Commons의 [DurationFormatUtils.formatDurationISO]를 사용합니다.
 * - 밀리초 단위로 환산한 값을 기반으로 포맷합니다.
 *
 * ```kotlin
 * val result = 1.asSeconds().formatISO()
 * // result == P0Y0M0DT0H0M1.000S
 * ```
 */
fun Duration.formatISO(): String = DurationFormatUtils.formatDurationISO(inMillis())

/**
 * [Duration]을 `HH:mm:ss.SSS` 형식 문자열로 포맷팅합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [DurationFormatUtils.formatDurationHMS]를 사용합니다.
 * - 밀리초 단위로 환산한 값을 기반으로 포맷합니다.
 *
 * ```kotlin
 * val d = 1.asHours() + 2.asMinutes() + 3.asSeconds() + 4.asMillis()
 * val result = d.formatHMS()
 * // result == 01:02:03.004
 * ```
 */
fun Duration.formatHMS(): String = DurationFormatUtils.formatDurationHMS(inMillis())

/**
 * [Duration]을 ISO Format의 문자열로 표현한 정규식
 */
private val durationIsoFormat: Regex =
    """P(?<year>\d+)Y(?<month>\d+)M(?<day>\d+)DT(?<hour>\d+)H(?<minute>\d+)M(?<second>\d+)\.(?<mills>\d{3})S""".toRegex()

/**
 * ISO-8601 형태로 표현된 Duration 문자열을 파싱해 [Duration]으로 변환합니다.
 *
 * ## 동작/계약
 * - 이 구현은 `년/월`을 무시하고(캡처만 하고 사용하지 않음), `일/시/분/초/밀리초`만으로 [Duration]을 구성합니다.
 * - 정규식과 매칭되지 않으면 null을 반환합니다.
 * - 입력 형식은 `P{Y}Y{M}M{D}DT{H}H{m}M{s}.{SSS}S` 형태를 기대합니다.
 *
 * ```kotlin
 * val d = parseIsoFormattedDuration("P1Y2M3DT4H5M6.007S")
 * // d == PT76H5M6.007S
 * ```
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
