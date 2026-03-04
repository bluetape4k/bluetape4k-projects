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
 * `ByteArray`를 암호화해 `BLOB`에 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 암호화 후 blob으로 변환합니다.
 * - 조회 시 blob bytes를 복호화해 원본 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val payload = table.encryptedBlob("payload", encryptor)
 * // payload.columnType.sqlType().contains("BLOB")
 * ```
 */
@Deprecated("use io.bluetape4k.exposed.core.tink.tinkDaeadBlob in bluetape4k-exposed-tink")
fun Table.encryptedBlob(
    name: String,
    encryptor: Encryptor,
): Column<ByteArray> =
    registerColumn(name, EncryptedBlobColumnType(encryptor))

/** `BLOB` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다. */
class EncryptedBlobColumnType(
    encryptor: Encryptor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), EncryptedBlobTransformer(encryptor))

/** `ByteArray` <-> `ExposedBlob` 암복호화 변환기입니다. */
class EncryptedBlobTransformer(private val encryptor: Encryptor): ColumnTransformer<ExposedBlob, ByteArray> {
    /** 엔티티 바이트를 암호화 blob으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob = encryptor.encrypt(value).toExposedBlob()

    /** DB blob을 복호화해 엔티티 바이트 배열로 변환합니다. */
    override fun wrap(value: ExposedBlob): ByteArray = encryptor.decrypt(value.bytes)

}
