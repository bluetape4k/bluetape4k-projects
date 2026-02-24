package io.bluetape4k.measured

import kotlin.math.pow

/**
 * 저장 용량 단위를 나타냅니다.
 */
open class Storage(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        private const val KILO: Double = 1024.0

        @JvmField val bytes: Storage = Storage("B")
        @JvmField val kiloBytes: Storage = Storage("KB", KILO)
        @JvmField val megaBytes: Storage = Storage("MB", KILO.pow(2))
        @JvmField val gigaBytes: Storage = Storage("GB", KILO.pow(3))
        @JvmField val teraBytes: Storage = Storage("TB", KILO.pow(4))
        @JvmField val petaBytes: Storage = Storage("PB", KILO.pow(5))
        @JvmField val exaBytes: Storage = Storage("EB", KILO.pow(6))
        @JvmField val zettaBytes: Storage = Storage("ZB", KILO.pow(7))
        @JvmField val yottaBytes: Storage = Storage("YB", KILO.pow(8))
    }
}

/**
 * 숫자를 바이트 단위 측정값으로 변환합니다.
 */
fun Number.bytes(): Measure<Storage> = this * Storage.bytes

/**
 * 숫자를 킬로바이트 단위 측정값으로 변환합니다.
 */
fun Number.kbytes(): Measure<Storage> = this * Storage.kiloBytes

/**
 * 숫자를 메가바이트 단위 측정값으로 변환합니다.
 */
fun Number.mbytes(): Measure<Storage> = this * Storage.megaBytes

/**
 * 숫자를 기가바이트 단위 측정값으로 변환합니다.
 */
fun Number.gbytes(): Measure<Storage> = this * Storage.gigaBytes

/**
 * 숫자를 테라바이트 단위 측정값으로 변환합니다.
 */
fun Number.tbytes(): Measure<Storage> = this * Storage.teraBytes

/**
 * 숫자를 페타바이트 단위 측정값으로 변환합니다.
 */
fun Number.pbytes(): Measure<Storage> = this * Storage.petaBytes
