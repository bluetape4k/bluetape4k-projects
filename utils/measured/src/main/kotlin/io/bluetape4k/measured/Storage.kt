package io.bluetape4k.measured

import io.bluetape4k.measured.Storage.Companion.bytes
import kotlin.math.pow

/**
 * 저장 용량 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 2진 접두사(`1024`)를 기준으로 단위 비율을 정의합니다.
 * - 기준 단위는 바이트([bytes])입니다.
 *
 * ```kotlin
 * val gb = 1.gbytes()
 * // gb `in` Storage.megaBytes == 1024.0
 * // gb `in` Storage.kiloBytes == 1048576.0
 * ```
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
 *
 * ```kotlin
 * val value = 1024.bytes()
 * // value `in` Storage.kiloBytes == 1.0
 * ```
 */
fun Number.bytes(): Measure<Storage> = this * Storage.bytes

/**
 * 숫자를 킬로바이트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.kbytes()
 * // value `in` Storage.bytes == 1024.0
 * ```
 */
fun Number.kbytes(): Measure<Storage> = this * Storage.kiloBytes

/**
 * 숫자를 메가바이트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.mbytes()
 * // value `in` Storage.kiloBytes == 1024.0
 * ```
 */
fun Number.mbytes(): Measure<Storage> = this * Storage.megaBytes

/**
 * 숫자를 기가바이트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.gbytes()
 * // value `in` Storage.megaBytes == 1024.0
 * ```
 */
fun Number.gbytes(): Measure<Storage> = this * Storage.gigaBytes

/**
 * 숫자를 테라바이트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.tbytes()
 * // value `in` Storage.gigaBytes == 1024.0
 * ```
 */
fun Number.tbytes(): Measure<Storage> = this * Storage.teraBytes

/**
 * 숫자를 페타바이트 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1.pbytes()
 * // value `in` Storage.teraBytes == 1024.0
 * ```
 */
fun Number.pbytes(): Measure<Storage> = this * Storage.petaBytes
