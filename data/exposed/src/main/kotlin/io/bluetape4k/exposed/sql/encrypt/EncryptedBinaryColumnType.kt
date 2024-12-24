package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.sql.BinaryColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.Table

/**
 * 엔티티 속성 값을 암호화하여 VARBINARY Column 으로 저장할 수 있는 Column 을 생성합니다.
 *
 * @sample io.bluetape4k.exposed.sql.encrypt.EncryptedColumnTypeTest.T1
 */
fun Table.encryptedBinary(
    name: String,
    length: Int = 255,
    encryptor: Encryptor = Encryptors.AES,
): Column<ByteArray> =
    registerColumn(name, EncryptedBinaryColumnType(encryptor, length))

class EncryptedBinaryColumnType(
    encryptor: Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), ByteArrayEncryptionTransformer(encryptor))

class ByteArrayEncryptionTransformer(
    private val encryptor: Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {

    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ByteArray {
        return encryptor.encrypt(value)
    }

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ByteArray): ByteArray {
        return encryptor.decrypt(value)
    }
}
