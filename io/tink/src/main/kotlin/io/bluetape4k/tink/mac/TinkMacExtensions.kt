package io.bluetape4k.tink.mac

/**
 * 바이트 배열에 대한 MAC 태그를 [TinkMac]으로 계산합니다.
 *
 * ```kotlin
 * val tag = "Hello".toByteArray().computeTinkMac(TinkMacs.HMAC_SHA256)
 * ```
 *
 * @param mac 사용할 [TinkMac] 인스턴스
 * @return 계산된 MAC 태그 바이트 배열
 */
fun ByteArray.computeTinkMac(mac: TinkMac): ByteArray = mac.computeMac(this)

/**
 * 바이트 배열에 대한 MAC 태그를 [TinkMac]으로 검증합니다.
 *
 * ```kotlin
 * val valid = "Hello".toByteArray().verifyTinkMac(tag, TinkMacs.HMAC_SHA256)
 * ```
 *
 * @param tag 검증할 MAC 태그
 * @param mac 사용할 [TinkMac] 인스턴스
 * @return 검증 성공 시 `true`, 실패 시 `false`
 */
fun ByteArray.verifyTinkMac(tag: ByteArray, mac: TinkMac): Boolean = mac.verifyMac(tag, this)

/**
 * 문자열에 대한 MAC 태그를 [TinkMac]으로 계산합니다.
 *
 * ```kotlin
 * val tag = "Hello, World!".computeTinkMac(TinkMacs.HMAC_SHA256)
 * ```
 *
 * @param mac 사용할 [TinkMac] 인스턴스
 * @return 계산된 MAC 태그 바이트 배열
 */
fun String.computeTinkMac(mac: TinkMac): ByteArray = mac.computeMac(this)

/**
 * 문자열에 대한 MAC 태그를 [TinkMac]으로 검증합니다.
 *
 * ```kotlin
 * val valid = "Hello, World!".verifyTinkMac(tag, TinkMacs.HMAC_SHA256)
 * ```
 *
 * @param tag 검증할 MAC 태그
 * @param mac 사용할 [TinkMac] 인스턴스
 * @return 검증 성공 시 `true`, 실패 시 `false`
 */
fun String.verifyTinkMac(tag: ByteArray, mac: TinkMac): Boolean = mac.verifyMac(tag, this)
