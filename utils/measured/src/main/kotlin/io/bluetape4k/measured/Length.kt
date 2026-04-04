package io.bluetape4k.measured

import io.bluetape4k.measured.Length.Companion.meters


/**
 * 길이 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 미터([meters])이며 각 단위는 미터 대비 비율로 정의됩니다.
 * - 단위 상수는 불변이며 변환 연산은 새 [Measure]를 반환합니다.
 *
 * ```kotlin
 * val km = 1.kilometers()
 * // km `in` Length.meters == 1000.0
 * ```
 */
open class Length(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val millimeters: Length = Length("mm", 0.001)
        @JvmField val centimeters: Length = Length("cm", 0.01)
        @JvmField val meters: Length = Length("m")
        @JvmField val kilometers: Length = Length("km", 1_000.0)
        @JvmField val inches: Length = Length("in", 0.0254)
        @JvmField val feet: Length = Length("ft", 0.3048)
        @JvmField val miles: Length = Length("mi", 1_609.344)
    }
}

/**
 * 숫자를 밀리미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 500.millimeters()
 * // value `in` Length.meters == 0.5
 * ```
 */
fun Number.millimeters(): Measure<Length> = this * Length.millimeters

/**
 * 숫자를 센티미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 100.centimeters()
 * // value `in` Length.meters == 1.0
 * ```
 */
fun Number.centimeters(): Measure<Length> = this * Length.centimeters

/**
 * 숫자를 미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.meters()
 * // value `in` Length.kilometers == 0.001
 * ```
 */
fun Number.meters(): Measure<Length> = this * Length.meters

/**
 * 숫자를 킬로미터 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 수치를 `km` 단위 [Measure]로 감쌉니다.
 *
 * ```kotlin
 * val value = 1.kilometers()
 * // value `in` Length.meters == 1000.0
 * ```
 */
fun Number.kilometers(): Measure<Length> = this * Length.kilometers

/**
 * 숫자를 인치 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.inches()
 * // value `in` Length.centimeters == 2.54
 * ```
 */
fun Number.inches(): Measure<Length> = this * Length.inches

/**
 * 숫자를 피트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.feet()
 * // value `in` Length.centimeters == 30.48
 * ```
 */
fun Number.feet(): Measure<Length> = this * Length.feet

/**
 * 숫자를 마일 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.miles()
 * // value `in` Length.kilometers == 1.609344
 * ```
 */
fun Number.miles(): Measure<Length> = this * Length.miles
