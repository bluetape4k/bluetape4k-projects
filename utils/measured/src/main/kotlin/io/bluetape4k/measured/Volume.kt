package io.bluetape4k.measured

import io.bluetape4k.measured.Volume.Companion.cubicMeters

/**
 * 부피 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 세제곱미터([cubicMeters])입니다.
 * - 리터/밀리리터는 SI 환산 비율(`1 L = 1e-3 m^3`)을 사용합니다.
 *
 * ```kotlin
 * val oneLiter = 1.liters()
 * // oneLiter `in` Volume.milliliters == 1000.0
 * ```
 */
open class Volume(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val cubicMillimeters: Volume = Volume("mm^3", 1.0e-9)

        @JvmField
        val cubicCentimeters: Volume = Volume("cm^3", 1.0e-6)

        @JvmField
        val milliliters: Volume = Volume("mL", 1.0e-6)

        @JvmField
        val liters: Volume = Volume("L", 1.0e-3)

        @JvmField
        val cubicMeters: Volume = Volume("m^3")
    }
}

/**
 * 숫자를 세제곱밀리미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1_000_000_000.cubicMillimeters()
 * // value `in` Volume.cubicMeters == 1.0
 * ```
 */
fun Number.cubicMillimeters(): Measure<Volume> = this * Volume.cubicMillimeters

/**
 * 숫자를 세제곱센티미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.cubicCentimeters()
 * // value `in` Volume.liters == 1.0
 * ```
 */
fun Number.cubicCentimeters(): Measure<Volume> = this * Volume.cubicCentimeters

/**
 * 숫자를 밀리리터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1000.milliliters()
 * // value `in` Volume.liters == 1.0
 * ```
 */
fun Number.milliliters(): Measure<Volume> = this * Volume.milliliters

/**
 * 숫자를 리터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 2.liters()
 * // value `in` Volume.milliliters == 2000.0
 * ```
 */
fun Number.liters(): Measure<Volume> = this * Volume.liters

/**
 * 숫자를 세제곱미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.cubicMeters()
 * // value `in` Volume.liters == 1000.0
 * ```
 */
fun Number.cubicMeters(): Measure<Volume> = this * Volume.cubicMeters

/**
 * 면적과 길이를 곱해 부피를 계산합니다.
 *
 * ## 동작/계약
 * - 면적(`m^2`)과 길이(`m`)를 곱해 `m^3`를 반환합니다.
 *
 * ```kotlin
 * val volume = 10.meters2() * 2.meters()
 * // volume `in` Volume.cubicMeters == 20.0
 * ```
 */
@JvmName("areaTimesLengthToVolume")
operator fun Measure<Area>.times(other: Measure<Length>): Measure<Volume> =
    ((this `in` Area.meters2) * (other `in` Length.meters)) * Volume.cubicMeters

/**
 * 길이와 면적을 곱해 부피를 계산합니다.
 *
 * ## 동작/계약
 * - 길이(`m`)와 면적(`m^2`)을 곱해 `m^3`를 반환합니다.
 *
 * ```kotlin
 * val volume = 2.meters() * 10.meters2()
 * // volume `in` Volume.cubicMeters == 20.0
 * ```
 */
@JvmName("lengthTimesAreaToVolume")
operator fun Measure<Length>.times(other: Measure<Area>): Measure<Volume> = other * this

/**
 * 부피를 면적으로 나눠 길이를 계산합니다.
 *
 * ## 동작/계약
 * - `m^3 / m^2 = m` 규칙으로 길이를 복원합니다.
 *
 * ```kotlin
 * val length = 20.cubicMeters() / 10.meters2()
 * // length `in` Length.meters == 2.0
 * ```
 */
@JvmName("volumeDivAreaToLength")
operator fun Measure<Volume>.div(other: Measure<Area>): Measure<Length> =
    ((this `in` Volume.cubicMeters) / (other `in` Area.meters2)) * Length.meters

/**
 * 부피를 길이로 나눠 면적을 계산합니다.
 *
 * ## 동작/계약
 * - `m^3 / m = m^2` 규칙으로 면적을 복원합니다.
 *
 * ```kotlin
 * val area = 20.cubicMeters() / 4.meters()
 * // area `in` Area.meters2 == 5.0
 * ```
 */
@JvmName("volumeDivLengthToArea")
operator fun Measure<Volume>.div(other: Measure<Length>): Measure<Area> =
    ((this `in` Volume.cubicMeters) / (other `in` Length.meters)) * Area.meters2
