package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.support.requirePositiveNumber
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
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(
    BinaryColumnType(length.requirePositiveNumber("length")),
    JasyptByteArrayEncryptionTransformer(encryptor)
)

/**
 * 바이트 배열 값의 DB 저장/조회 시 암호화 및 복호화를 수행합니다.
 */
class JasyptByteArrayEncryptionTransformer(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {
    /**
     * Encrypts the given value using the provided [encryptor].
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    override fun unwrap(value: ByteArray) = encryptor.encrypt(value)

    /**
     * Decrypts the given value using the provided [encryptor].
     */
    override fun wrap(value: ByteArray) = encryptor.decrypt(value)
}
