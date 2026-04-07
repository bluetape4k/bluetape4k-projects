package io.bluetape4k.measured

import io.bluetape4k.measured.Energy.Companion.joules
import io.bluetape4k.measured.Power.Companion.watts

/**
 * 에너지 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 줄([joules])입니다.
 * - Wh/kWh는 초 단위 에너지 환산(`1 Wh = 3600 J`) 비율을 사용합니다.
 *
 * ```kotlin
 * val e = 1.kiloWattHours()
 * // e `in` Energy.joules == 3600000.0
 * ```
 */
open class Energy(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val joules: Energy = Energy("J")

        @JvmField
        val kiloJoules: Energy = Energy("kJ", 1.0e3)

        @JvmField
        val megaJoules: Energy = Energy("MJ", 1.0e6)

        @JvmField
        val wattHours: Energy = Energy("Wh", 3_600.0)

        @JvmField
        val kiloWattHours: Energy = Energy("kWh", 3_600_000.0)
    }
}

/**
 * 전력 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 와트([watts])입니다.
 * - 접두 단위는 SI 비율(10^3, 10^6, 10^9)을 사용합니다.
 *
 * ```kotlin
 * val p = 1500.watts()
 * // p.toHuman() == "1.5 kW"
 * ```
 */
open class Power(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val watts: Power = Power("W")

        @JvmField
        val milliWatts: Power = Power("mW", 1.0e-3)

        @JvmField
        val kiloWatts: Power = Power("kW", 1.0e3)

        @JvmField
        val megaWatts: Power = Power("MW", 1.0e6)

        @JvmField
        val gigaWatts: Power = Power("GW", 1.0e9)
    }
}

/**
 * 숫자를 줄 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 3600.joules()
 * // value `in` Energy.wattHours == 1.0
 * ```
 */
fun Number.joules(): Measure<Energy> = this * Energy.joules

/**
 * 숫자를 킬로줄 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kiloJoules()
 * // value `in` Energy.joules == 1000.0
 * ```
 */
fun Number.kiloJoules(): Measure<Energy> = this * Energy.kiloJoules

/**
 * 숫자를 메가줄 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.megaJoules()
 * // value `in` Energy.kiloJoules == 1000.0
 * ```
 */
fun Number.megaJoules(): Measure<Energy> = this * Energy.megaJoules

/**
 * 숫자를 와트시 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.wattHours()
 * // value `in` Energy.joules == 3600.0
 * ```
 */
fun Number.wattHours(): Measure<Energy> = this * Energy.wattHours

/**
 * 숫자를 킬로와트시 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kiloWattHours()
 * // value `in` Energy.joules == 3600000.0
 * ```
 */
fun Number.kiloWattHours(): Measure<Energy> = this * Energy.kiloWattHours

/**
 * 숫자를 와트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.watts()
 * // value `in` Power.kiloWatts == 1.0
 * ```
 */
fun Number.watts(): Measure<Power> = this * Power.watts

/**
 * 숫자를 밀리와트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.milliWatts()
 * // value `in` Power.watts == 1.0
 * ```
 */
fun Number.milliWatts(): Measure<Power> = this * Power.milliWatts

/**
 * 숫자를 킬로와트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kiloWatts()
 * // value `in` Power.watts == 1000.0
 * ```
 */
fun Number.kiloWatts(): Measure<Power> = this * Power.kiloWatts

/**
 * 숫자를 메가와트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.megaWatts()
 * // value `in` Power.kiloWatts == 1000.0
 * ```
 */
fun Number.megaWatts(): Measure<Power> = this * Power.megaWatts

/**
 * 숫자를 기가와트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.gigaWatts()
 * // value `in` Power.megaWatts == 1000.0
 * ```
 */
fun Number.gigaWatts(): Measure<Power> = this * Power.gigaWatts

/**
 * 전력과 시간을 곱해 에너지를 계산합니다.
 *
 * ## 동작/계약
 * - `W * s = J` 규칙으로 계산해 [Energy.joules] 단위로 반환합니다.
 *
 * ```kotlin
 * val energy = 2.kiloWatts() * 3.hours()
 * // energy `in` Energy.kiloWattHours == 6.0
 * ```
 */
@JvmName("powerTimesTimeToEnergy")
operator fun Measure<Power>.times(other: Measure<Time>): Measure<Energy> =
    ((this `in` Power.watts) * (other `in` Time.seconds)) * Energy.joules

/**
 * 시간과 전력을 곱해 에너지를 계산합니다.
 *
 * ## 동작/계약
 * - `s * W = J` 규칙으로 계산해 [Energy.joules] 단위로 반환합니다.
 *
 * ```kotlin
 * val energy = 3.hours() * 2.kiloWatts()
 * // energy `in` Energy.kiloWattHours == 6.0
 * ```
 */
@JvmName("timeTimesPowerToEnergy")
operator fun Measure<Time>.times(other: Measure<Power>): Measure<Energy> = other * this

/**
 * 에너지를 시간으로 나눠 전력을 계산합니다.
 *
 * ## 동작/계약
 * - `J / s = W` 규칙으로 계산해 [Power.watts] 단위로 반환합니다.
 *
 * ```kotlin
 * val power = 6.kiloWattHours() / 2.hours()
 * // power `in` Power.kiloWatts == 3.0
 * ```
 */
@JvmName("energyDivTimeToPower")
operator fun Measure<Energy>.div(other: Measure<Time>): Measure<Power> =
    ((this `in` Energy.joules) / (other `in` Time.seconds)) * Power.watts
