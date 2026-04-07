package io.bluetape4k.measured

import io.bluetape4k.measured.Mass.Companion.grams


/**
 * 질량 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 그램([grams])이며 각 단위는 그램 대비 비율로 정의됩니다.
 * - 단위 상수는 불변이며 변환 연산은 새 [Measure]를 반환합니다.
 *
 * ```kotlin
 * val kg = 5.kilograms()
 * // kg `in` Mass.grams == 5000.0
 * ```
 */
open class Mass(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val grams: Mass = Mass("g")

        @JvmField
        val kilograms: Mass = Mass("kg", 1_000.0)

        @JvmField
        val tons: Mass = Mass("ton", 1_000_000.0)
    }
}

/**
 * 숫자를 그램 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.grams()
 * // value `in` Mass.kilograms == 1.0
 * ```
 */
fun Number.grams(): Measure<Mass> = this * Mass.grams

/**
 * 숫자를 킬로그램 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 5.kilograms()
 * // value `in` Mass.grams == 5000.0
 * ```
 */
fun Number.kilograms(): Measure<Mass> = this * Mass.kilograms

/**
 * 숫자를 톤 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 2.tons()
 * // value `in` Mass.kilograms == 2000.0
 * ```
 */
fun Number.tons(): Measure<Mass> = this * Mass.tons
