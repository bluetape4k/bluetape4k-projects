package io.bluetape4k.measured

import io.bluetape4k.measured.Frequency.Companion.hertz


/**
 * 주파수 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 헤르츠([hertz])이며 각 단위는 SI 비율(10^3, 10^6, 10^9)로 정의됩니다.
 * - 단위 상수는 불변이며 변환 연산은 새 [Measure]를 반환합니다.
 *
 * ```kotlin
 * val freq = 2.4.gigaHertz()
 * // freq `in` Frequency.megaHertz == 2400.0
 * ```
 */
open class Frequency(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val hertz: Frequency = Frequency("Hz")

        @JvmField
        val kiloHertz: Frequency = Frequency("kHz", 1.0e3)

        @JvmField
        val megaHertz: Frequency = Frequency("MHz", 1.0e6)

        @JvmField
        val gigaHertz: Frequency = Frequency("GHz", 1.0e9)
    }
}

/**
 * 숫자를 헤르츠 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.hertz()
 * // value `in` Frequency.kiloHertz == 1.0
 * ```
 */
fun Number.hertz(): Measure<Frequency> = this * Frequency.hertz

/**
 * 숫자를 킬로헤르츠 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kiloHertz()
 * // value `in` Frequency.hertz == 1000.0
 * ```
 */
fun Number.kiloHertz(): Measure<Frequency> = this * Frequency.kiloHertz

/**
 * 숫자를 메가헤르츠 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.megaHertz()
 * // value `in` Frequency.kiloHertz == 1000.0
 * ```
 */
fun Number.megaHertz(): Measure<Frequency> = this * Frequency.megaHertz

/**
 * 숫자를 기가헤르츠 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.gigaHertz()
 * // value `in` Frequency.megaHertz == 1000.0
 * ```
 */
fun Number.gigaHertz(): Measure<Frequency> = this * Frequency.gigaHertz
