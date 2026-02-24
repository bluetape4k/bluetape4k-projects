package io.bluetape4k.measured

import kotlin.jvm.JvmName

/**
 * 부피 단위를 나타냅니다.
 */
open class Volume(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val cubicMillimeters: Volume = Volume("mm^3", 1.0e-9)
        @JvmField val cubicCentimeters: Volume = Volume("cm^3", 1.0e-6)
        @JvmField val milliliters: Volume = Volume("mL", 1.0e-6)
        @JvmField val liters: Volume = Volume("L", 1.0e-3)
        @JvmField val cubicMeters: Volume = Volume("m^3")
    }
}

/**
 * 숫자를 세제곱밀리미터 단위 측정값으로 변환합니다.
 */
fun Number.cubicMillimeters(): Measure<Volume> = this * Volume.cubicMillimeters

/**
 * 숫자를 세제곱센티미터 단위 측정값으로 변환합니다.
 */
fun Number.cubicCentimeters(): Measure<Volume> = this * Volume.cubicCentimeters

/**
 * 숫자를 밀리리터 단위 측정값으로 변환합니다.
 */
fun Number.milliliters(): Measure<Volume> = this * Volume.milliliters

/**
 * 숫자를 리터 단위 측정값으로 변환합니다.
 */
fun Number.liters(): Measure<Volume> = this * Volume.liters

/**
 * 숫자를 세제곱미터 단위 측정값으로 변환합니다.
 */
fun Number.cubicMeters(): Measure<Volume> = this * Volume.cubicMeters

/**
 * 면적과 길이를 곱해 부피를 계산합니다.
 */
@JvmName("areaTimesLengthToVolume")
operator fun Measure<Area>.times(other: Measure<Length>): Measure<Volume> =
    ((this `in` Area.meters2) * (other `in` Length.meters)) * Volume.cubicMeters

/**
 * 길이와 면적을 곱해 부피를 계산합니다.
 */
@JvmName("lengthTimesAreaToVolume")
operator fun Measure<Length>.times(other: Measure<Area>): Measure<Volume> = other * this

/**
 * 부피를 면적으로 나눠 길이를 계산합니다.
 */
@JvmName("volumeDivAreaToLength")
operator fun Measure<Volume>.div(other: Measure<Area>): Measure<Length> =
    ((this `in` Volume.cubicMeters) / (other `in` Area.meters2)) * Length.meters

/**
 * 부피를 길이로 나눠 면적을 계산합니다.
 */
@JvmName("volumeDivLengthToArea")
operator fun Measure<Volume>.div(other: Measure<Length>): Measure<Area> =
    ((this `in` Volume.cubicMeters) / (other `in` Length.meters)) * Area.meters2
