package io.bluetape4k.measured

import io.bluetape4k.measured.Area.Companion.meters2

/**
 * 면적 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 제곱미터([meters2])입니다.
 * - 길이 제곱 연산 결과와 호환되는 단위 비율을 제공합니다.
 *
 * ```kotlin
 * val square = 10_000.centimeters2()
 * // square `in` Area.meters2 == 1.0
 * ```
 */
open class Area(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val millimeters2: Area = Area("mm^2", 1.0e-6)

        @JvmField
        val centimeters2: Area = Area("cm^2", 1.0e-4)

        @JvmField
        val meters2: Area = Area("m^2")

        @JvmField
        val kilometers2: Area = Area("km^2", 1.0e6)
    }
}

/**
 * 숫자를 제곱밀리미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1_000_000.millimeters2()
 * // value `in` Area.meters2 == 1.0
 * ```
 */
fun Number.millimeters2(): Measure<Area> = this * Area.millimeters2

/**
 * 숫자를 제곱센티미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 10_000.centimeters2()
 * // value `in` Area.meters2 == 1.0
 * ```
 */
fun Number.centimeters2(): Measure<Area> = this * Area.centimeters2

/**
 * 숫자를 제곱미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.meters2()
 * // value `in` Area.centimeters2 == 10000.0
 * ```
 */
fun Number.meters2(): Measure<Area> = this * Area.meters2

/**
 * 숫자를 제곱킬로미터 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kilometers2()
 * // value `in` Area.meters2 == 1000000.0
 * ```
 */
fun Number.kilometers2(): Measure<Area> = this * Area.kilometers2

/**
 * 길이 두 값을 곱해 면적을 계산합니다.
 *
 * ## 동작/계약
 * - 두 길이를 미터로 환산해 곱한 뒤 `m^2` 단위로 반환합니다.
 * - 수신 객체를 변경하지 않고 새 [Measure]를 반환합니다.
 *
 * ```kotlin
 * val area = 10.meters() * 2.meters()
 * // area `in` Area.meters2 == 20.0
 * ```
 */
@JvmName("timesLengthToArea")
operator fun Measure<Length>.times(other: Measure<Length>): Measure<Area> =
    ((this `in` Length.meters) * (other `in` Length.meters)) * Area.meters2
