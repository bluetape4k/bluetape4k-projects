package io.bluetape4k.exposed.sql.jasypt

import org.jetbrains.exposed.sql.BinaryColumnType
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform

class JasyptBinaryColumnType(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), JasyptByteArrayEncryptionTransformer(encryptor))

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
