package io.bluetape4k.measured

/**
 * 주파수 단위를 나타냅니다.
 */
open class Frequency(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val hertz: Frequency = Frequency("Hz")
        @JvmField val kiloHertz: Frequency = Frequency("kHz", 1.0e3)
        @JvmField val megaHertz: Frequency = Frequency("MHz", 1.0e6)
        @JvmField val gigaHertz: Frequency = Frequency("GHz", 1.0e9)
    }
}

/**
 * 숫자를 헤르츠 단위 측정값으로 변환합니다.
 */
fun Number.hertz(): Measure<Frequency> = this * Frequency.hertz

/**
 * 숫자를 킬로헤르츠 단위 측정값으로 변환합니다.
 */
fun Number.kiloHertz(): Measure<Frequency> = this * Frequency.kiloHertz

/**
 * 숫자를 메가헤르츠 단위 측정값으로 변환합니다.
 */
fun Number.megaHertz(): Measure<Frequency> = this * Frequency.megaHertz

/**
 * 숫자를 기가헤르츠 단위 측정값으로 변환합니다.
 */
fun Number.gigaHertz(): Measure<Frequency> = this * Frequency.gigaHertz
