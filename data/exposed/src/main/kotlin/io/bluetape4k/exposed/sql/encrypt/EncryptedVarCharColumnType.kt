package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType

/**
 * 엔티티 속성 값을 암호화하여 VARCHAR Column 으로 저장할 수 있는 Column 을 생성합니다.
 *
 * @sample io.bluetape4k.exposed.sql.encrypt.EncryptedColumnTypeTest.T1
 */
fun Table.encryptedVarChar(
    name: String,
    colLength: Int = 255,
    encryptor: Encryptor = Encryptors.AES,
): Column<String> =
    registerColumn(name, EncryptedVarCharColumnType(encryptor, colLength))

class EncryptedVarCharColumnType(
    encryptor: Encryptor,
    colLength: Int,
): ColumnWithTransform<String, String>(VarCharColumnType(colLength), StringEncryptionTransformer(encryptor))

class StringEncryptionTransformer(private val encryptor: Encryptor): ColumnTransformer<String, String> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: String): String {
        return encryptor.encrypt(value)
    }

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): String {
        return encryptor.decrypt(value)
    }
}
