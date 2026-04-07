package io.bluetape4k.measured

import io.bluetape4k.measured.Pressure.Companion.pascal


/**
 * 압력 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 파스칼([pascal])입니다.
 * - bar/atm/psi 등은 고정 환산 비율을 사용합니다.
 *
 * ```kotlin
 * val atm = 1.atm()
 * // atm `in` Pressure.pascal == 101325.0
 * ```
 */
open class Pressure(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        /** 파스칼을 기준 단위로 사용합니다. */
        @JvmField
        val pascal: Pressure = Pressure("Pa")

        @JvmField
        val hectoPascal: Pressure = Pressure("hPa", 100.0)

        @JvmField
        val kiloPascal: Pressure = Pressure("kPa", 1_000.0)

        @JvmField
        val megaPascal: Pressure = Pressure("MPa", 1_000_000.0)

        @JvmField
        val gigaPascal: Pressure = Pressure("GPa", 1_000_000_000.0)

        @JvmField
        val bar: Pressure = Pressure("bar", 100_000.0)

        @JvmField
        val deciBar: Pressure = Pressure("dbar", 10_000.0)

        @JvmField
        val milliBar: Pressure = Pressure("mbar", 100.0)

        @JvmField
        val atmosphere: Pressure = Pressure("atm", 101_325.0)

        @JvmField
        val psi: Pressure = Pressure("psi", 6_894.757)

        @JvmField
        val torr: Pressure = Pressure("torr", 101_325.0 / 760.0)

        @JvmField
        val mmHg: Pressure = Pressure("mmHg", 101_325.0 / 760.0)
    }
}

/**
 * 숫자를 파스칼 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 101325.pascal()
 * // value `in` Pressure.atmosphere == 1.0
 * ```
 */
fun Number.pascal(): Measure<Pressure> = this * Pressure.pascal

/**
 * 숫자를 헥토파스칼 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1013.25.hectoPascal()
 * // value `in` Pressure.atmosphere == 1.0
 * ```
 */
fun Number.hectoPascal(): Measure<Pressure> = this * Pressure.hectoPascal

/**
 * 숫자를 킬로파스칼 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 100.kiloPascal()
 * // value `in` Pressure.bar == 1.0
 * ```
 */
fun Number.kiloPascal(): Measure<Pressure> = this * Pressure.kiloPascal

/**
 * 숫자를 메가파스칼 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.megaPascal()
 * // value `in` Pressure.kiloPascal == 1000.0
 * ```
 */
fun Number.megaPascal(): Measure<Pressure> = this * Pressure.megaPascal

/**
 * 숫자를 바 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.bar()
 * // value `in` Pressure.pascal == 100000.0
 * ```
 */
fun Number.bar(): Measure<Pressure> = this * Pressure.bar

/**
 * 숫자를 표준대기압 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - `atm` 단위 [Measure]를 생성합니다.
 *
 * ```kotlin
 * val value = 1.atm()
 * // value `in` Pressure.pascal == 101325.0
 * ```
 */
fun Number.atm(): Measure<Pressure> = this * Pressure.atmosphere

/**
 * 숫자를 psi 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 14.696.psi()
 * // value `in` Pressure.atmosphere == 1.0 (근사값)
 * ```
 */
fun Number.psi(): Measure<Pressure> = this * Pressure.psi
