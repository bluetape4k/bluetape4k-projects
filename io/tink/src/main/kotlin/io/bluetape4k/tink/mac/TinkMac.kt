package io.bluetape4k.tink.mac

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.Mac
import io.bluetape4k.tink.macKeysetHandle

/**
 * Google Tink [Mac] 프리미티브를 Kotlin 관용적으로 래핑한 MAC(메시지 인증 코드) 클래스입니다.
 *
 * HMAC-SHA256, HMAC-SHA512 등 MAC 알고리즘을 지원합니다.
 * 데이터 무결성 검증 및 인증에 사용합니다.
 *
 * ```kotlin
 * val mac = TinkMac(macKeysetHandle())
 * val tag = mac.computeMac("Hello, World!".toByteArray())
 * val isValid = mac.verifyMac(tag, "Hello, World!".toByteArray())
 * // isValid == true
 * ```
 *
 * @param keysetHandle Tink [KeysetHandle] — `macKeysetHandle()` 팩토리 함수로 생성
 */
class TinkMac(keysetHandle: KeysetHandle = macKeysetHandle()) {

    private val mac: Mac by lazy {
        keysetHandle.getPrimitive(Mac::class.java)
    }

    /**
     * 바이트 배열에 대한 MAC 태그를 계산합니다.
     *
     * @param data MAC을 계산할 데이터
     * @return 계산된 MAC 태그 바이트 배열
     */
    fun computeMac(data: ByteArray): ByteArray = mac.computeMac(data)

    /**
     * 문자열에 대한 MAC 태그를 계산합니다.
     *
     * @param data MAC을 계산할 문자열 (UTF-8 인코딩)
     * @return 계산된 MAC 태그 바이트 배열
     */
    fun computeMac(data: String): ByteArray = computeMac(data.toByteArray(Charsets.UTF_8))

    /**
     * 바이트 배열에 대한 MAC 태그를 검증합니다.
     *
     * @param tag 검증할 MAC 태그
     * @param data 원본 데이터
     * @return 검증 성공 시 `true`, 실패 시 `false`
     */
    fun verifyMac(tag: ByteArray, data: ByteArray): Boolean = try {
        mac.verifyMac(tag, data)
        true
    } catch (_: Exception) {
        false
    }

    /**
     * 문자열에 대한 MAC 태그를 검증합니다.
     *
     * @param tag 검증할 MAC 태그
     * @param data 원본 문자열 (UTF-8 인코딩)
     * @return 검증 성공 시 `true`, 실패 시 `false`
     */
    fun verifyMac(tag: ByteArray, data: String): Boolean =
        verifyMac(tag, data.toByteArray(Charsets.UTF_8))
}
