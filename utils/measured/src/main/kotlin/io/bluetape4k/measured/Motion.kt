package io.bluetape4k.measured

/**
 * 속도/가속도 관련 단위 상수를 제공합니다.
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
 */
fun Number.metersPerSecond(): Measure<Velocity> = this * MotionUnits.metersPerSecond

/**
 * 숫자를 km/h 단위 측정값으로 변환합니다.
 */
fun Number.kilometersPerHour(): Measure<Velocity> = this * MotionUnits.kilometersPerHour

/**
 * 숫자를 m/s^2 단위 측정값으로 변환합니다.
 */
fun Number.metersPerSecondSquared(): Measure<Acceleration> = this * MotionUnits.metersPerSecondSquared

/**
 * 속도를 m/s 값으로 반환합니다.
 */
fun Measure<Velocity>.inMetersPerSecond(): Double = this `in` MotionUnits.metersPerSecond

/**
 * 속도를 km/h 값으로 반환합니다.
 */
fun Measure<Velocity>.inKilometersPerHour(): Double = this `in` MotionUnits.kilometersPerHour

/**
 * 가속도를 m/s^2 값으로 반환합니다.
 */
fun Measure<Acceleration>.inMetersPerSecondSquared(): Double = this `in` MotionUnits.metersPerSecondSquared
