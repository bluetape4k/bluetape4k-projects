package io.bluetape4k.measured

/**
 * 전압 단위를 나타냅니다.
 *
 * 기준 단위는 볼트([volts])입니다.
 */
open class Voltage(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val volts: Voltage = Voltage("V")

        @JvmField
        val milliVolts: Voltage = Voltage("mV", 1.0e-3)

        @JvmField
        val kiloVolts: Voltage = Voltage("kV", 1.0e3)
    }
}

/**
 * 전기 저항 단위를 나타냅니다.
 *
 * 기준 단위는 옴([ohms])입니다.
 */
open class Resistance(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val ohms: Resistance = Resistance("Ω")

        @JvmField
        val milliOhms: Resistance = Resistance("mΩ", 1.0e-3)

        @JvmField
        val kiloOhms: Resistance = Resistance("kΩ", 1.0e3)

        @JvmField
        val megaOhms: Resistance = Resistance("MΩ", 1.0e6)
    }
}

/** 숫자를 볼트 단위 측정값으로 변환합니다. */
fun Number.volts(): Measure<Voltage> = this * Voltage.volts

/** 숫자를 밀리볼트 단위 측정값으로 변환합니다. */
fun Number.milliVolts(): Measure<Voltage> = this * Voltage.milliVolts

/** 숫자를 킬로볼트 단위 측정값으로 변환합니다. */
fun Number.kiloVolts(): Measure<Voltage> = this * Voltage.kiloVolts

/** 숫자를 옴 단위 측정값으로 변환합니다. */
fun Number.ohms(): Measure<Resistance> = this * Resistance.ohms

/** 숫자를 밀리옴 단위 측정값으로 변환합니다. */
fun Number.milliOhms(): Measure<Resistance> = this * Resistance.milliOhms

/** 숫자를 킬로옴 단위 측정값으로 변환합니다. */
fun Number.kiloOhms(): Measure<Resistance> = this * Resistance.kiloOhms

/** 숫자를 메가옴 단위 측정값으로 변환합니다. */
fun Number.megaOhms(): Measure<Resistance> = this * Resistance.megaOhms

/** 전력을 전류로 나눠 전압을 계산합니다. */
@JvmName("powerDivCurrentToVoltage")
operator fun Measure<Power>.div(other: Measure<Current>): Measure<Voltage> =
    ((this `in` Power.watts) / (other `in` Current.amps)) * Voltage.volts

/** 전압을 전류로 나눠 저항을 계산합니다. */
@JvmName("voltageDivCurrentToResistance")
operator fun Measure<Voltage>.div(other: Measure<Current>): Measure<Resistance> =
    ((this `in` Voltage.volts) / (other `in` Current.amps)) * Resistance.ohms
