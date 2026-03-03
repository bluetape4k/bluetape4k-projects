package io.bluetape4k.tink.daead

import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeysetHandle
import io.bluetape4k.tink.EMPTY_BYTES
import io.bluetape4k.tink.daeadKeysetHandle
import java.util.Base64

/**
 * Google Tink [DeterministicAead] 프리미티브를 Kotlin 관용적으로 래핑한 결정적 AEAD 암호화 클래스입니다.
 *
 * 동일한 평문과 연관 데이터에 대해 항상 동일한 암호문을 생성합니다.
 * 데이터베이스 필드 암호화나 인덱스 검색이 필요한 경우에 사용합니다.
 * AES256-SIV 알고리즘을 기본으로 사용합니다.
 *
 * **주의**: 같은 키로 같은 평문을 반복 암호화하면 동일한 암호문이 나오므로,
 * 패턴 유출 가능성이 있습니다. 이 특성이 필요하지 않다면 [TinkAead]를 사용하세요.
 *
 * ```kotlin
 * val daead = TinkDeterministicAead(daeadKeysetHandle())
 * val ct1 = daead.encryptDeterministically("Hello".toByteArray())
 * val ct2 = daead.encryptDeterministically("Hello".toByteArray())
 * // ct1 contentEquals ct2 == true
 * ```
 *
 * @param keysetHandle Tink [KeysetHandle] — `daeadKeysetHandle()` 팩토리 함수로 생성
 */
class TinkDeterministicAead(keysetHandle: KeysetHandle = daeadKeysetHandle()) {

    private val daead: DeterministicAead by lazy {
        keysetHandle.getPrimitive(DeterministicAead::class.java)
    }

    /**
     * 바이트 배열을 결정적으로 암호화합니다.
     *
     * @param plaintext 암호화할 평문 바이트 배열
     * @param associatedData 인증에 사용할 연관 데이터 (기본값: 빈 배열)
     * @return 암호화된 바이트 배열 (동일 입력 시 항상 동일한 출력)
     */
    fun encryptDeterministically(plaintext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
        daead.encryptDeterministically(plaintext, associatedData)

    /**
     * 결정적으로 암호화된 바이트 배열을 복호화합니다.
     *
     * @param ciphertext 복호화할 암호문 바이트 배열
     * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
     * @return 복호화된 평문 바이트 배열
     * @throws com.google.crypto.tink.shaded.protobuf.GeneralSecurityException 복호화 실패 또는 인증 실패 시
     */
    fun decryptDeterministically(ciphertext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
        daead.decryptDeterministically(ciphertext, associatedData)

    /**
     * 문자열을 결정적으로 암호화합니다. 암호문은 Base64 인코딩 문자열로 반환됩니다.
     *
     * @param plaintext 암호화할 평문 문자열 (UTF-8)
     * @param associatedData 인증에 사용할 연관 데이터 (기본값: 빈 배열)
     * @return Base64 인코딩된 암호문 문자열 (동일 입력 시 항상 동일한 출력)
     */
    fun encryptDeterministically(plaintext: String, associatedData: ByteArray = EMPTY_BYTES): String {
        val cipherBytes = encryptDeterministically(plaintext.toByteArray(Charsets.UTF_8), associatedData)
        return Base64.getEncoder().encodeToString(cipherBytes)
    }

    /**
     * Base64 인코딩된 암호문 문자열을 결정적으로 복호화합니다.
     *
     * @param ciphertext Base64 인코딩된 암호문 문자열
     * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
     * @return 복호화된 평문 문자열 (UTF-8)
     * @throws com.google.crypto.tink.shaded.protobuf.GeneralSecurityException 복호화 실패 또는 인증 실패 시
     */
    fun decryptDeterministically(ciphertext: String, associatedData: ByteArray = EMPTY_BYTES): String {
        val cipherBytes = Base64.getDecoder().decode(ciphertext)
        return decryptDeterministically(cipherBytes, associatedData).toString(Charsets.UTF_8)
    }
}
