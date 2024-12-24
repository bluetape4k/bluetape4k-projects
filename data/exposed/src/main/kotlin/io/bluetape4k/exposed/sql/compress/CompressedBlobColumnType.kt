package io.bluetape4k.exposed.sql.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.sql.BlobColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

/**
 * 엔티티 속성 값을 압축하여 BLOB Column 으로 저장할 수 있는 Column 을 생성합니다.
 *
 * @sample io.bluetape4k.exposed.sql.compress.CompressedBlobColumnTypeTest.T1
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
    override fun unwrap(value: ByteArray): ExposedBlob {
        return ExposedBlob(compressor.compress(value))
    }

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray {
        return compressor.decompress(value.bytes)
    }
}
