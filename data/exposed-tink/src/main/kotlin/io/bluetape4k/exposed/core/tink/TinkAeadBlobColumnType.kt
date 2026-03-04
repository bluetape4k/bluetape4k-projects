package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tink.aead.TinkAead
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/** `BLOB` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다. */
class TinkAeadBlobColumnType(
    private val encryptor: TinkAead,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), TinkAeadBlobTransformer(encryptor))

/** `ByteArray` <-> `ExposedBlob` 암복호화 변환기입니다. */
class TinkAeadBlobTransformer(private val encryptor: TinkAead):
    ColumnTransformer<ExposedBlob, ByteArray> {
    companion object: KLogging()

    /** 엔티티 바이트를 암호화 blob으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob {
        log.debug { "AEAD 바이너리 암호화 중: size=${value.size}" }
        return encryptor.encrypt(value, ByteArray(0)).apply {
            log.debug { "AEAD 바이너리 암호화 완료: size=${this.size}" }
        }.toExposedBlob()
    }

    /** DB blob을 복호화해 엔티티 바이트 배열로 변환합니다. */
    override fun wrap(value: ExposedBlob): ByteArray {
        log.debug { "AEAD 바이너리 복호화 중: size=${value.bytes.size}" }
        return encryptor.decrypt(value.bytes, ByteArray(0)).apply {
            log.debug { "AEAD 바이너리 복호화 완료: size=${this.size}" }
        }
    }
}
