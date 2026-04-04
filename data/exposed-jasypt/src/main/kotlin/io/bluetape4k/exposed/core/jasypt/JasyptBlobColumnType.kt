package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * `BLOB` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [JasyptBlobTransformer.unwrap]로 암호화된 [ExposedBlob]을 저장합니다.
 * - 조회 시 [JasyptBlobTransformer.wrap]으로 복호화된 바이트 배열을 반환합니다.
 * - `BLOB` 컬럼은 길이 제한이 없으므로 대용량 바이너리 데이터 암호화에 적합합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("blob_table") {
 *     val data = jasyptBlob("data", Encryptors.AES).nullable()
 * }
 * val id = T1.insertAndGetId { it[data] = "hello".toUtf8Bytes() }
 * val restored = T1.selectAll().where { T1.id eq id }.single()[T1.data]!!
 * // restored.toUtf8String() == "hello"
 * ```
 *
 * @param encryptor 바이트 배열 암/복호화를 수행할 암호화기입니다.
 */
class JasyptBlobColumnType(
    encryptor: Encryptor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), JasyptBlobTransformer(encryptor))

/**
 * `ByteArray` <-> `ExposedBlob` 암복호화 변환기입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 바이트 배열을 암호화해 [ExposedBlob]으로 변환합니다.
 * - [wrap]은 DB에서 읽은 [ExposedBlob]의 암호문 바이트를 복호화해 원본 바이트 배열로 복원합니다.
 * - round-trip(`wrap(unwrap(x))`)은 원본 바이트 배열과 동일합니다.
 *
 * ```kotlin
 * val transformer = JasyptBlobTransformer(Encryptors.AES)
 * val source = "jasypt-blob-source".toUtf8Bytes()
 * val restored = transformer.wrap(transformer.unwrap(source))
 * // restored.contentEquals(source) == true
 * ```
 *
 * @param encryptor 바이트 배열 암/복호화 구현체입니다.
 */
class JasyptBlobTransformer(private val encryptor: Encryptor): ColumnTransformer<ExposedBlob, ByteArray> {

    /**
     * 평문 바이트 배열을 암호화된 [ExposedBlob]으로 변환합니다.
     *
     * ```kotlin
     * val transformer = JasyptBlobTransformer(Encryptors.AES)
     * val blob = transformer.unwrap("jasypt-blob-source".toUtf8Bytes())
     * // blob is ExposedBlob
     * ```
     *
     * @param value 암호화할 평문 바이트 배열입니다.
     */
    override fun unwrap(value: ByteArray): ExposedBlob = encryptor.encrypt(value).toExposedBlob()

    /**
     * DB에서 읽은 [ExposedBlob]을 복호화해 원본 바이트 배열로 변환합니다.
     *
     * ```kotlin
     * val transformer = JasyptBlobTransformer(Encryptors.AES)
     * val source = "jasypt-blob-source".toUtf8Bytes()
     * val restored = transformer.wrap(transformer.unwrap(source))
     * // restored.contentEquals(source) == true
     * ```
     *
     * @param value 복호화할 [ExposedBlob]입니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray = encryptor.decrypt(value.bytes)

}
