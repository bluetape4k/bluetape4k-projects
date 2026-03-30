package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aead.TinkAead
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * 바이트 배열 값을 Google Tink AEAD로 암호화해서 `BLOB` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [TinkAeadBlobTransformer.unwrap]로 암호화된 [ExposedBlob]을 저장합니다.
 * - 조회 시 [TinkAeadBlobTransformer.wrap]으로 복호화된 바이트 배열을 반환합니다.
 * - AEAD는 비결정적이므로 같은 평문이라도 매번 다른 암호문이 저장되며, 조건 검색/인덱스 검색에는 사용할 수 없습니다.
 *
 * @param encryptor Tink AEAD 암/복호화를 수행할 인스턴스입니다.
 */
class TinkAeadBlobColumnType(
    private val encryptor: TinkAead,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), TinkAeadBlobTransformer(encryptor))

/**
 * `ByteArray` <-> `ExposedBlob` 저장 경계에서 Tink AEAD 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 바이트 배열을 암호화해 [ExposedBlob]으로 변환합니다.
 * - [wrap]은 DB에서 읽은 [ExposedBlob]의 암호문 바이트를 복호화해 원본 바이트 배열로 복원합니다.
 *
 * @param encryptor Tink AEAD 암/복호화 인스턴스입니다.
 */
class TinkAeadBlobTransformer(private val encryptor: TinkAead):
    ColumnTransformer<ExposedBlob, ByteArray> {
    companion object: KLogging()

    /** 평문 바이트 배열을 암호화된 [ExposedBlob]으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob {
        return encryptor.encrypt(value).toExposedBlob()
    }

    /** DB에서 읽은 [ExposedBlob]을 복호화해 원본 바이트 배열로 변환합니다. */
    override fun wrap(value: ExposedBlob): ByteArray {
        return encryptor.decrypt(value.bytes)
    }
}
