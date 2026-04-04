package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.daead.TinkDeterministicAead
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * 바이트 배열 값을 Google Tink Deterministic AEAD로 암호화해서 `BLOB` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [TinkDaeadBlobTransformer.unwrap]로 결정적 암호화를 수행한 [ExposedBlob]을 저장합니다.
 * - 조회 시 [TinkDaeadBlobTransformer.wrap]으로 복호화된 바이트 배열을 반환합니다.
 * - 결정적 암호화이므로 같은 평문은 항상 같은 암호문으로 저장되어, equality 기반 검색 시나리오에 활용할 수 있습니다.
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화를 수행할 인스턴스입니다.
 */
class TinkDaeadBlobColumnType(
    private val encryptor: TinkDeterministicAead,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), TinkDaeadBlobTransformer(encryptor))

/**
 * `ByteArray` <-> `ExposedBlob` 저장 경계에서 Tink Deterministic AEAD 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 바이트 배열을 결정적으로 암호화해 [ExposedBlob]으로 변환합니다.
 * - [wrap]은 DB에서 읽은 [ExposedBlob]의 암호문 바이트를 복호화해 원본 바이트 배열로 복원합니다.
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화 인스턴스입니다.
 */
class TinkDaeadBlobTransformer(private val encryptor: TinkDeterministicAead):
    ColumnTransformer<ExposedBlob, ByteArray> {
    companion object: KLogging()

    /**
     * 평문 바이트 배열을 결정적으로 암호화한 [ExposedBlob]으로 변환합니다.
     *
     * ```kotlin
     * val transformer = TinkDaeadBlobTransformer(TinkDaeads.AES256_SIV)
     * val blob1 = transformer.unwrap("deterministic-source".toByteArray())
     * val blob2 = transformer.unwrap("deterministic-source".toByteArray())
     * // blob1.bytes.contentEquals(blob2.bytes) == true (결정적 암호화)
     * ```
     *
     * @param value 암호화할 평문 바이트 배열입니다.
     */
    override fun unwrap(value: ByteArray): ExposedBlob {
        return encryptor.encryptDeterministically(value).toExposedBlob()
    }

    /**
     * DB에서 읽은 [ExposedBlob]을 복호화해 원본 바이트 배열로 변환합니다.
     *
     * ```kotlin
     * val transformer = TinkDaeadBlobTransformer(TinkDaeads.AES256_SIV)
     * val source = "tink-daead-blob-source".toByteArray()
     * val restored = transformer.wrap(transformer.unwrap(source))
     * // restored.contentEquals(source) == true
     * ```
     *
     * @param value 복호화할 [ExposedBlob]입니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray {
        return encryptor.decryptDeterministically(value.bytes)
    }
}
