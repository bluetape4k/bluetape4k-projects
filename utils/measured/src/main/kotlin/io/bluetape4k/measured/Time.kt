package io.bluetape4k.measured

/**
 * 시간 단위를 나타냅니다.
 */
open class Time(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val milliseconds: Time = Time("ms")
        @JvmField val seconds: Time = Time("s", 1_000.0)
        @JvmField val minutes: Time = Time("min", 60_000.0)
        @JvmField val hours: Time = Time("hr", 3_600_000.0)
    }
}

/**
 * 숫자를 밀리초 단위 측정값으로 변환합니다.
 */
fun Number.milliseconds(): Measure<Time> = this * Time.milliseconds

/**
 * 숫자를 초 단위 측정값으로 변환합니다.
 */
fun Number.seconds(): Measure<Time> = this * Time.seconds

/**
 * 숫자를 분 단위 측정값으로 변환합니다.
 */
fun Number.minutes(): Measure<Time> = this * Time.minutes

/**
 * 숫자를 시간 단위 측정값으로 변환합니다.
 */
fun Number.hours(): Measure<Time> = this * Time.hours
