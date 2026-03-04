package io.bluetape4k.tink.aead

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import io.bluetape4k.tink.EMPTY_BYTES
import io.bluetape4k.tink.aeadKeysetHandle
import java.util.*

/**
 * Google Tink [Aead] 프리미티브를 Kotlin 관용적으로 래핑한 AEAD 암호화 클래스입니다.
 *
 * AES-GCM, ChaCha20-Poly1305 등 인증 암호화(AEAD) 알고리즘을 지원합니다.
 * String 입출력 시 내부적으로 UTF-8 인코딩과 Base64 변환을 처리합니다.
 *
 * ```kotlin
 * val tinkAead = TinkAead(aeadKeysetHandle())
 * val ciphertext = tinkAead.encrypt("비밀 메시지".toByteArray())
 * val plaintext = tinkAead.decrypt(ciphertext)
 * ```
 *
 * @param keysetHandle Tink [KeysetHandle] — `aeadKeysetHandle()` 팩토리 함수로 생성
 */
class TinkAead(keysetHandle: KeysetHandle = aeadKeysetHandle()) {

    private val aead: Aead by lazy {
        keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    /**
     * 바이트 배열을 AEAD 암호화합니다.
     *
     * @param plaintext 암호화할 평문 바이트 배열
     * @param associatedData 인증에 사용할 연관 데이터 (암호화되지 않음, 기본값: 빈 배열)
     * @return 암호화된 바이트 배열
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
        aead.encrypt(plaintext, associatedData)

    /**
     * 암호화된 바이트 배열을 AEAD 복호화합니다.
     *
     * @param ciphertext 복호화할 암호문 바이트 배열
     * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
     * @return 복호화된 평문 바이트 배열
     * @throws com.google.crypto.tink.shaded.protobuf.GeneralSecurityException 복호화 실패 또는 인증 실패 시
     */
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
        aead.decrypt(ciphertext, associatedData)

    /**
     * 문자열을 AEAD 암호화합니다. 암호문은 Base64 인코딩 문자열로 반환됩니다.
     *
     * @param plaintext 암호화할 평문 문자열 (UTF-8)
     * @param associatedData 인증에 사용할 연관 데이터 (기본값: 빈 배열)
     * @return Base64 인코딩된 암호문 문자열
     */
    fun encrypt(plaintext: String, associatedData: ByteArray = EMPTY_BYTES): String {
        val cipherBytes = encrypt(plaintext.toByteArray(Charsets.UTF_8), associatedData)
        return Base64.getEncoder().encodeToString(cipherBytes)
    }

    /**
     * Base64 인코딩된 암호문 문자열을 AEAD 복호화합니다.
     *
     * @param ciphertext Base64 인코딩된 암호문 문자열
     * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
     * @return 복호화된 평문 문자열 (UTF-8)
     * @throws com.google.crypto.tink.shaded.protobuf.GeneralSecurityException 복호화 실패 또는 인증 실패 시
     */
    fun decrypt(ciphertext: String, associatedData: ByteArray = EMPTY_BYTES): String {
        val cipherBytes = Base64.getDecoder().decode(ciphertext)
        return decrypt(cipherBytes, associatedData).toString(Charsets.UTF_8)
    }
}
