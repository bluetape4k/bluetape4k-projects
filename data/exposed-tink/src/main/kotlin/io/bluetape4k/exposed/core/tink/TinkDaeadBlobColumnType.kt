package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tink.daead.TinkDeterministicAead
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/** `BLOB` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다. */
class TinkDaeadBlobColumnType(
    private val encryptor: TinkDeterministicAead,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), TinkDaeadBlobTransformer(encryptor))

/** `ByteArray` <-> `ExposedBlob` 암복호화 변환기입니다. */
class TinkDaeadBlobTransformer(private val encryptor: TinkDeterministicAead):
    ColumnTransformer<ExposedBlob, ByteArray> {
    companion object: KLogging()

    /** 엔티티 바이트를 암호화 blob으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob {
        log.debug { "DAEAD 바이너리 암호화 중: size=${value.size}" }
        return encryptor.encryptDeterministically(value, ByteArray(0)).apply {
            log.debug { "DAEAD 바이너리 암호화 완료: size=${this.size}" }
        }.toExposedBlob()
    }

    /** DB blob을 복호화해 엔티티 바이트 배열로 변환합니다. */
    override fun wrap(value: ExposedBlob): ByteArray {
        log.debug { "DAEAD 바이너리 복호화 중: size=${value.bytes.size}" }
        return encryptor.decryptDeterministically(value.bytes, ByteArray(0)).apply {
            log.debug { "DAEAD 바이너리 복호화 완료: size=${this.size}" }
        }
    }
}
