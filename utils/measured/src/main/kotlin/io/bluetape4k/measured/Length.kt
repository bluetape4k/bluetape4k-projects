package io.bluetape4k.measured

/**
 * 길이 단위를 나타냅니다.
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
 */
fun Number.millimeters(): Measure<Length> = this * Length.millimeters

/**
 * 숫자를 센티미터 단위 측정값으로 변환합니다.
 */
fun Number.centimeters(): Measure<Length> = this * Length.centimeters

/**
 * 숫자를 미터 단위 측정값으로 변환합니다.
 */
fun Number.meters(): Measure<Length> = this * Length.meters

/**
 * 숫자를 킬로미터 단위 측정값으로 변환합니다.
 */
fun Number.kilometers(): Measure<Length> = this * Length.kilometers

/**
 * 숫자를 인치 단위 측정값으로 변환합니다.
 */
fun Number.inches(): Measure<Length> = this * Length.inches

/**
 * 숫자를 피트 단위 측정값으로 변환합니다.
 */
fun Number.feet(): Measure<Length> = this * Length.feet

/**
 * 숫자를 마일 단위 측정값으로 변환합니다.
 */
fun Number.miles(): Measure<Length> = this * Length.miles
