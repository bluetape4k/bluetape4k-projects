package io.bluetape4k.exposed.core.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * 엔티티 속성 값을 암호화하여 BLOB Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.encryptedBlob(
    name: String,
    encryptor: Encryptor,
): Column<ByteArray> =
    registerColumn(name, EncryptedBlobColumnType(encryptor))

/**
 * 엔티티 속성 값을 암호화하여 BLOB Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
class EncryptedBlobColumnType(
    encryptor: Encryptor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), EncryptedBlobTransformer(encryptor))

/**
 * [ExposedBlob] 컬럼 타입을 암호화된 BLOB 타입으로 변환합니다.
 */
class EncryptedBlobTransformer(private val encryptor: Encryptor): ColumnTransformer<ExposedBlob, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ExposedBlob = encryptor.encrypt(value).toExposedBlob()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray = encryptor.decrypt(value.bytes)

}
