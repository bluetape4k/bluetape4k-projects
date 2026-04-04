package io.bluetape4k.measured

/**
 * 속도 단위 별칭입니다.
 *
 * ## 동작/계약
 * - [Length] / [Time] 조합 단위로 m/s, km/h 등을 표현합니다.
 *
 * ```kotlin
 * val speed = 10.metersPerSecond()
 * // speed `in` MotionUnits.metersPerSecond == 10.0
 * ```
 */
typealias Velocity = UnitsRatio<Length, Time>

/**
 * 가속도 단위 별칭입니다.
 *
 * ## 동작/계약
 * - [Length] / [Square]<[Time]> 조합 단위로 m/s^2 등을 표현합니다.
 *
 * ```kotlin
 * val accel = 9.8.metersPerSecondSquared()
 * // accel `in` MotionUnits.metersPerSecondSquared == 9.8
 * ```
 */
typealias Acceleration = UnitsRatio<Length, Square<Time>>
