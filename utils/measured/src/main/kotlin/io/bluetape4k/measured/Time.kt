package io.bluetape4k.measured

/**
 * 시간 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 밀리초([milliseconds])이며 각 단위는 밀리초 대비 비율로 정의됩니다.
 * - 단위 상수는 불변이며 변환 연산은 새 [Measure]를 반환합니다.
 *
 * ```kotlin
 * val t = 1.hours()
 * // t `in` Time.minutes == 60.0
 * ```
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
 *
 * ```kotlin
 * val value = 1000.milliseconds()
 * // value `in` Time.seconds == 1.0
 * ```
 */
fun Number.milliseconds(): Measure<Time> = this * Time.milliseconds

/**
 * 숫자를 초 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 60.seconds()
 * // value `in` Time.minutes == 1.0
 * ```
 */
fun Number.seconds(): Measure<Time> = this * Time.seconds

/**
 * 숫자를 분 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 90.minutes()
 * // value `in` Time.hours == 1.5
 * ```
 */
fun Number.minutes(): Measure<Time> = this * Time.minutes

/**
 * 숫자를 시간 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 2.hours()
 * // value `in` Time.minutes == 120.0
 * ```
 */
fun Number.hours(): Measure<Time> = this * Time.hours
