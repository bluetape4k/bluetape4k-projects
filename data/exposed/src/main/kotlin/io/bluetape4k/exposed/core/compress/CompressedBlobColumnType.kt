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
 * 엔티티 속성 값을 압축하여 BLOB Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.compressedBlob(
    name: String,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBlobColumnType(compressor))

class CompressedBlobColumnType(
    compressor: Compressor,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), CompressedBlobTransformer(compressor))

class CompressedBlobTransformer(private val compressor: Compressor): ColumnTransformer<ExposedBlob, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ExposedBlob = compressor.compress(value).toExposedBlob()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray = compressor.decompress(value.bytes)
}
