package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform

/**
 * 바이트 배열 값을 암호화하여 `VARBINARY` 컬럼에 저장하는 컬럼 타입입니다.
 *
 * @param encryptor 바이트 배열 암/복호화를 수행할 암호화기
 * @param length 암호문을 저장할 컬럼 길이 (0보다 커야 함)
 */
class JasyptBinaryColumnType(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(
    BinaryColumnType(length),
    ByteArrayJasyptEncryptionTransformer(encryptor)
)

/**
 * 바이트 배열 값의 DB 저장/조회 시 암호화 및 복호화를 수행합니다.
 */
class ByteArrayJasyptEncryptionTransformer(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {

    companion object: KLogging()

    /**
     * Encrypts the given value using the provided [encryptor].
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    override fun unwrap(value: ByteArray): ByteArray {
        log.debug { "Encrypting value=${value.contentToString()}" }
        return encryptor.encrypt(value).apply {
            log.debug { "Encrypted value=${this.contentToString()}" }
        }
    }

    /**
     * Decrypts the given value using the provided [encryptor].
     */
    override fun wrap(value: ByteArray): ByteArray {
        log.debug { "Decrypting value=${value.contentToString()}" }
        return encryptor.decrypt(value).apply {
            log.debug { "Decrypted value=${this.contentToString()}" }
        }
    }
}
