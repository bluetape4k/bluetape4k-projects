package io.bluetape4k.measured

/**
 * 속도/가속도 관련 단위 상수를 제공합니다.
 *
 * ## 동작/계약
 * - 속도는 [Velocity], 가속도는 [Acceleration] 단위 별칭을 사용합니다.
 * - 각 상수는 단위 연산으로 조합된 불변 단위입니다.
 *
 * ```kotlin
 * val v = MotionUnits.kilometersPerHour
 * // v.suffix == "km/hr"
 * ```
 */
object MotionUnits {
    /** m/s 단위입니다. */
    val metersPerSecond: Velocity = Length.meters / Time.seconds

    /** km/h 단위입니다. */
    val kilometersPerHour: Velocity = Length.kilometers / Time.hours

    /** m/s^2 단위입니다. */
    val metersPerSecondSquared: Acceleration = Length.meters / (Time.seconds * Time.seconds)
}

/**
 * 숫자를 m/s 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 새 [Measure]를 생성합니다.
 *
 * ```kotlin
 * val speed = 10.metersPerSecond()
 * // speed.inMetersPerSecond() == 10.0
 * ```
 */
fun Number.metersPerSecond(): Measure<Velocity> = this * MotionUnits.metersPerSecond

/**
 * 숫자를 km/h 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 단위는 `km/hr`입니다.
 *
 * ```kotlin
 * val speed = 36.kilometersPerHour()
 * // speed.inMetersPerSecond() == 10.0
 * ```
 */
fun Number.kilometersPerHour(): Measure<Velocity> = this * MotionUnits.kilometersPerHour

/**
 * 숫자를 m/s^2 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 단위는 `m/s*s` 조합 가속도 단위를 사용합니다.
 *
 * ```kotlin
 * val a = 9.8.metersPerSecondSquared()
 * // a.inMetersPerSecondSquared() == 9.8
 * ```
 */
fun Number.metersPerSecondSquared(): Measure<Acceleration> = this * MotionUnits.metersPerSecondSquared

/**
 * 속도를 m/s 값으로 반환합니다.
 *
 * ```kotlin
 * val speed = 36.kilometersPerHour()
 * // speed.inMetersPerSecond() == 10.0
 * ```
 */
fun Measure<Velocity>.inMetersPerSecond(): Double = this `in` MotionUnits.metersPerSecond

/**
 * 속도를 km/h 값으로 반환합니다.
 *
 * ```kotlin
 * val speed = 10.metersPerSecond()
 * // speed.inKilometersPerHour() == 36.0
 * ```
 */
fun Measure<Velocity>.inKilometersPerHour(): Double = this `in` MotionUnits.kilometersPerHour

/**
 * 가속도를 m/s^2 값으로 반환합니다.
 *
 * ```kotlin
 * val accel = 9.8.metersPerSecondSquared()
 * // accel.inMetersPerSecondSquared() == 9.8
 * ```
 */
fun Measure<Acceleration>.inMetersPerSecondSquared(): Double = this `in` MotionUnits.metersPerSecondSquared
