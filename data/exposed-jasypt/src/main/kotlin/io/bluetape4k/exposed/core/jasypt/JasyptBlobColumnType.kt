package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/** `BLOB` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다. */
class JasyptBlobColumnType(
    encryptor: Encryptor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), JasyptBlobTransformer(encryptor))

/** `ByteArray` <-> `ExposedBlob` 암복호화 변환기입니다. */
class JasyptBlobTransformer(private val encryptor: Encryptor): ColumnTransformer<ExposedBlob, ByteArray> {
    /** 엔티티 바이트를 암호화 blob으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob = encryptor.encrypt(value).toExposedBlob()

    /** DB blob을 복호화해 엔티티 바이트 배열로 변환합니다. */
    override fun wrap(value: ExposedBlob): ByteArray = encryptor.decrypt(value.bytes)

}
