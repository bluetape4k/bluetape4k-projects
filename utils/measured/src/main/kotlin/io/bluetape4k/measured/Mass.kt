package io.bluetape4k.measured

/**
 * 질량 단위를 나타냅니다.
 */
open class Mass(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val grams: Mass = Mass("g")
        @JvmField val kilograms: Mass = Mass("kg", 1_000.0)
        @JvmField val tons: Mass = Mass("ton", 1_000_000.0)
    }
}

/**
 * 숫자를 그램 단위 측정값으로 변환합니다.
 */
fun Number.grams(): Measure<Mass> = this * Mass.grams

/**
 * 숫자를 킬로그램 단위 측정값으로 변환합니다.
 */
fun Number.kilograms(): Measure<Mass> = this * Mass.kilograms

/**
 * 숫자를 톤 단위 측정값으로 변환합니다.
 */
fun Number.tons(): Measure<Mass> = this * Mass.tons
