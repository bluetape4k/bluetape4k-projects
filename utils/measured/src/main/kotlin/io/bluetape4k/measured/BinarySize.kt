package io.bluetape4k.measured

/**
 * 디지털 데이터 크기/대역폭 단위를 나타냅니다.
 *
 * 10진(SI) 계열과 2진(IEC) 계열, bit 계열 단위를 함께 제공합니다.
 */
open class BinarySize(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField val bytes: BinarySize = BinarySize("B")

        // SI (10^3)
        @JvmField val kiloBytes: BinarySize = BinarySize("kB", 1_000.0)
        @JvmField val megaBytes: BinarySize = BinarySize("MB", 1_000.0 * kiloBytes.ratio)
        @JvmField val gigaBytes: BinarySize = BinarySize("GB", 1_000.0 * megaBytes.ratio)
        @JvmField val teraBytes: BinarySize = BinarySize("TB", 1_000.0 * gigaBytes.ratio)
        @JvmField val petaBytes: BinarySize = BinarySize("PB", 1_000.0 * teraBytes.ratio)

        // IEC (2^10)
        @JvmField val kibiBytes: BinarySize = BinarySize("KiB", 1_024.0)
        @JvmField val mebiBytes: BinarySize = BinarySize("MiB", 1_024.0 * kibiBytes.ratio)
        @JvmField val gibiBytes: BinarySize = BinarySize("GiB", 1_024.0 * mebiBytes.ratio)
        @JvmField val tebiBytes: BinarySize = BinarySize("TiB", 1_024.0 * gibiBytes.ratio)
        @JvmField val pebiBytes: BinarySize = BinarySize("PiB", 1_024.0 * tebiBytes.ratio)

        // bit
        @JvmField val bits: BinarySize = BinarySize("bit", 1.0 / 8.0)
        @JvmField val kiloBits: BinarySize = BinarySize("kbit", 1_000.0 * bits.ratio)
        @JvmField val megaBits: BinarySize = BinarySize("Mbit", 1_000.0 * kiloBits.ratio)
        @JvmField val gigaBits: BinarySize = BinarySize("Gbit", 1_000.0 * megaBits.ratio)
        @JvmField val teraBits: BinarySize = BinarySize("Tbit", 1_000.0 * gigaBits.ratio)
        @JvmField val petaBits: BinarySize = BinarySize("Pbit", 1_000.0 * teraBits.ratio)
    }
}

/**
 * 숫자를 byte 단위 측정값으로 변환합니다.
 */
fun Number.binaryBytes(): Measure<BinarySize> = this * BinarySize.bytes

/**
 * 숫자를 kilobyte(10진) 단위 측정값으로 변환합니다.
 */
fun Number.kilobytes10(): Measure<BinarySize> = this * BinarySize.kiloBytes

/**
 * 숫자를 megabyte(10진) 단위 측정값으로 변환합니다.
 */
fun Number.megabytes10(): Measure<BinarySize> = this * BinarySize.megaBytes

/**
 * 숫자를 gigabyte(10진) 단위 측정값으로 변환합니다.
 */
fun Number.gigabytes10(): Measure<BinarySize> = this * BinarySize.gigaBytes

/**
 * 숫자를 kibibyte(2진) 단위 측정값으로 변환합니다.
 */
fun Number.kibiBytes(): Measure<BinarySize> = this * BinarySize.kibiBytes

/**
 * 숫자를 mebibyte(2진) 단위 측정값으로 변환합니다.
 */
fun Number.mebiBytes(): Measure<BinarySize> = this * BinarySize.mebiBytes

/**
 * 숫자를 gibibyte(2진) 단위 측정값으로 변환합니다.
 */
fun Number.gibiBytes(): Measure<BinarySize> = this * BinarySize.gibiBytes

/**
 * 숫자를 bit 단위 측정값으로 변환합니다.
 */
fun Number.bits(): Measure<BinarySize> = this * BinarySize.bits

/**
 * 숫자를 kilobit 단위 측정값으로 변환합니다.
 */
fun Number.kiloBits(): Measure<BinarySize> = this * BinarySize.kiloBits
