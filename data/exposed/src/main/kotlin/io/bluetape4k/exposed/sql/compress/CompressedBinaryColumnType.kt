package io.bluetape4k.exposed.sql.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.sql.BinaryColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.Table

/**
 * 엔티티 속성 값을 압축하여 VARBINARY Column 으로 저장할 수 있는 Column 을 생성합니다.
 *
 * @sample io.bluetape4k.exposed.sql.compress.CompressedBinaryColumnTypeTest.T1
 */
fun Table.compressedBinary(
    name: String,
    length: Int,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBinaryColumnType(compressor, length))

class CompressedBinaryColumnType(
    compressor: Compressor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), CompressedBinaryTransformer(compressor))

class CompressedBinaryTransformer(
    private val compressor: Compressor,
): ColumnTransformer<ByteArray, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ByteArray = compressor.compress(value)

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ByteArray): ByteArray = compressor.decompress(value)
}
