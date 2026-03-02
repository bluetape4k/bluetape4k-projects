package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform

/**
 * 바이트 배열 값을 암호화해서 `VARBINARY` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [ByteArrayJasyptEncryptionTransformer.unwrap]로 암호화하고, 조회 시 `wrap`으로 복호화합니다.
 * - 입력 배열을 직접 수정하지 않고 암호화/복호화 결과 배열을 새로 반환합니다.
 * - 컬럼 길이 검증은 생성 전에 호출되는 `jasyptBinary(...).requirePositiveNumber(...)` 경로에서 수행됩니다.
 * - 테스트처럼 `wrap(unwrap(x))` round-trip은 원본 바이트 배열과 동일합니다.
 *
 * ```kotlin
 * val transformer = ByteArrayJasyptEncryptionTransformer(Encryptors.RC4)
 * val source = "jasypt-binary-source".toUtf8Bytes()
 * val restored = transformer.wrap(transformer.unwrap(source))
 * // restored.contentEquals(source) == true
 * ```
 *
 * @param encryptor 바이트 배열 암/복호화를 수행할 암호화기입니다.
 * @param length 암호문 바이트를 저장할 컬럼 길이입니다.
 */
class JasyptBinaryColumnType(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(
    BinaryColumnType(length),
    ByteArrayJasyptEncryptionTransformer(encryptor)
)

/**
 * 바이너리 컬럼의 저장/조회 경계에서 Jasypt 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 바이트 배열을 암호문 바이트 배열로 변환합니다.
 * - [wrap]은 DB에서 읽은 암호문 바이트 배열을 복호화해 원본 바이트 배열로 변환합니다.
 * - round-trip(`wrap(unwrap(x))`)은 테스트 기준으로 입력 배열과 동일합니다.
 *
 * ```kotlin
 * val transformer = ByteArrayJasyptEncryptionTransformer(Encryptors.RC4)
 * val source = "jasypt-binary-source".toUtf8Bytes()
 * val restored = transformer.wrap(transformer.unwrap(source))
 * // restored.contentEquals(source) == true
 * ```
 *
 * @param encryptor 바이트 배열 암/복호화 구현체입니다.
 */
class ByteArrayJasyptEncryptionTransformer(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {

    companion object: KLogging()

    /**
     * 평문 바이트 배열을 암호문 바이트 배열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 암호화합니다.
     * - 입력 배열을 직접 변경하지 않고 암호문 배열을 반환합니다.
     *
     * ```kotlin
     * val transformer = ByteArrayJasyptEncryptionTransformer(Encryptors.RC4)
     * val encrypted = transformer.unwrap("jasypt-binary-source".toUtf8Bytes())
     * // encrypted.isNotEmpty() == true
     * ```
     *
     * @param value 암호화할 평문 바이트 배열입니다.
     */
    override fun unwrap(value: ByteArray): ByteArray {
        log.debug { "Encrypting value=${value.contentToString()}" }
        return encryptor.encrypt(value).apply {
            log.debug { "Encrypted value=${this.contentToString()}" }
        }
    }

    /**
     * 암호문 바이트 배열을 복호화해 평문 바이트 배열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 복호화합니다.
     * - 암호문 형식이 아니거나 키가 맞지 않으면 encryptor 구현 예외가 전파됩니다.
     *
     * ```kotlin
     * val transformer = ByteArrayJasyptEncryptionTransformer(Encryptors.RC4)
     * val source = "jasypt-binary-source".toUtf8Bytes()
     * val restored = transformer.wrap(transformer.unwrap(source))
     * // restored.contentEquals(source) == true
     * ```
     *
     * @param value 복호화할 암호문 바이트 배열입니다.
     */
    override fun wrap(value: ByteArray): ByteArray {
        log.debug { "Decrypting value=${value.contentToString()}" }
        return encryptor.decrypt(value).apply {
            log.debug { "Decrypted value=${this.contentToString()}" }
        }
    }
}
