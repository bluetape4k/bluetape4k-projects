package io.bluetape4k.measured

import kotlin.jvm.JvmName

/**
 * 에너지 단위를 나타냅니다.
 */
open class Energy(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val joules: Energy = Energy("J")
        @JvmField val kiloJoules: Energy = Energy("kJ", 1.0e3)
        @JvmField val megaJoules: Energy = Energy("MJ", 1.0e6)
        @JvmField val wattHours: Energy = Energy("Wh", 3_600.0)
        @JvmField val kiloWattHours: Energy = Energy("kWh", 3_600_000.0)
    }
}

/**
 * 전력 단위를 나타냅니다.
 */
open class Power(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val watts: Power = Power("W")
        @JvmField val milliWatts: Power = Power("mW", 1.0e-3)
        @JvmField val kiloWatts: Power = Power("kW", 1.0e3)
        @JvmField val megaWatts: Power = Power("MW", 1.0e6)
        @JvmField val gigaWatts: Power = Power("GW", 1.0e9)
    }
}

/**
 * 숫자를 줄 단위 측정값으로 변환합니다.
 */
fun Number.joules(): Measure<Energy> = this * Energy.joules

/**
 * 숫자를 킬로줄 단위 측정값으로 변환합니다.
 */
fun Number.kiloJoules(): Measure<Energy> = this * Energy.kiloJoules

/**
 * 숫자를 메가줄 단위 측정값으로 변환합니다.
 */
fun Number.megaJoules(): Measure<Energy> = this * Energy.megaJoules

/**
 * 숫자를 와트시 단위 측정값으로 변환합니다.
 */
fun Number.wattHours(): Measure<Energy> = this * Energy.wattHours

/**
 * 숫자를 킬로와트시 단위 측정값으로 변환합니다.
 */
fun Number.kiloWattHours(): Measure<Energy> = this * Energy.kiloWattHours

/**
 * 숫자를 와트 단위 측정값으로 변환합니다.
 */
fun Number.watts(): Measure<Power> = this * Power.watts

/**
 * 숫자를 밀리와트 단위 측정값으로 변환합니다.
 */
fun Number.milliWatts(): Measure<Power> = this * Power.milliWatts

/**
 * 숫자를 킬로와트 단위 측정값으로 변환합니다.
 */
fun Number.kiloWatts(): Measure<Power> = this * Power.kiloWatts

/**
 * 숫자를 메가와트 단위 측정값으로 변환합니다.
 */
fun Number.megaWatts(): Measure<Power> = this * Power.megaWatts

/**
 * 숫자를 기가와트 단위 측정값으로 변환합니다.
 */
fun Number.gigaWatts(): Measure<Power> = this * Power.gigaWatts

/**
 * 전력과 시간을 곱해 에너지를 계산합니다.
 */
@JvmName("powerTimesTimeToEnergy")
operator fun Measure<Power>.times(other: Measure<Time>): Measure<Energy> =
    ((this `in` Power.watts) * (other `in` Time.seconds)) * Energy.joules

/**
 * 시간과 전력을 곱해 에너지를 계산합니다.
 */
@JvmName("timeTimesPowerToEnergy")
operator fun Measure<Time>.times(other: Measure<Power>): Measure<Energy> = other * this

/**
 * 에너지를 시간으로 나눠 전력을 계산합니다.
 */
@JvmName("energyDivTimeToPower")
operator fun Measure<Energy>.div(other: Measure<Time>): Measure<Power> =
    ((this `in` Energy.joules) / (other `in` Time.seconds)) * Power.watts
