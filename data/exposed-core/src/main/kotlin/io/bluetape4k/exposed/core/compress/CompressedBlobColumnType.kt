package io.bluetape4k.exposed.core.compress

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * `ByteArray`를 압축해 `BLOB`에 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 압축 후 [org.jetbrains.exposed.v1.core.statements.api.ExposedBlob]으로 감싸서 저장합니다.
 * - 조회 시 blob bytes를 복원해 원본 `ByteArray`를 반환합니다.
 *
 * ```kotlin
 * val content = table.compressedBlob("content")
 * // content.columnType.sqlType().contains("BLOB")
 * ```
 */
fun Table.compressedBlob(
    name: String,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBlobColumnType(compressor))

/** `BLOB` + 압축 변환기를 결합한 컬럼 타입입니다. */
class CompressedBlobColumnType(
    compressor: Compressor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), CompressedBlobTransformer(compressor))

/** `ByteArray` <-> `ExposedBlob` 압축/복원 변환기입니다. */
class CompressedBlobTransformer(private val compressor: Compressor): ColumnTransformer<ExposedBlob, ByteArray> {
    /** 엔티티 값을 압축 blob으로 변환합니다. */
    override fun unwrap(value: ByteArray): ExposedBlob = compressor.compress(value).toExposedBlob()

    /** DB blob을 원본 바이트 배열로 복원합니다. */
    override fun wrap(value: ExposedBlob): ByteArray = compressor.decompress(value.bytes)
}
