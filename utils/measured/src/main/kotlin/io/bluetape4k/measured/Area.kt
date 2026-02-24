package io.bluetape4k.measured

import kotlin.jvm.JvmName

/**
 * 면적 단위를 나타냅니다.
 */
open class Area(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val millimeters2: Area = Area("mm^2", 1.0e-6)
        @JvmField val centimeters2: Area = Area("cm^2", 1.0e-4)
        @JvmField val meters2: Area = Area("m^2")
        @JvmField val kilometers2: Area = Area("km^2", 1.0e6)
    }
}

/**
 * 숫자를 제곱밀리미터 단위 측정값으로 변환합니다.
 */
fun Number.millimeters2(): Measure<Area> = this * Area.millimeters2

/**
 * 숫자를 제곱센티미터 단위 측정값으로 변환합니다.
 */
fun Number.centimeters2(): Measure<Area> = this * Area.centimeters2

/**
 * 숫자를 제곱미터 단위 측정값으로 변환합니다.
 */
fun Number.meters2(): Measure<Area> = this * Area.meters2

/**
 * 숫자를 제곱킬로미터 단위 측정값으로 변환합니다.
 */
fun Number.kilometers2(): Measure<Area> = this * Area.kilometers2

/**
 * 길이 두 값을 곱해 면적을 계산합니다.
 */
@JvmName("timesLengthToArea")
operator fun Measure<Length>.times(other: Measure<Length>): Measure<Area> =
    ((this `in` Length.meters) * (other `in` Length.meters)) * Area.meters2
