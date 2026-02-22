package io.bluetape4k.exposed.core.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table

/**
 * 엔티티 속성 값을 압축하여 VARBINARY Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.compressedBinary(
    name: String,
    length: Int,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBinaryColumnType(compressor, length))

/**
 * [ByteArray] 값을 압축해 `VARBINARY` 컬럼에 저장하는 컬럼 타입입니다.
 */
class CompressedBinaryColumnType(
    compressor: Compressor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), CompressedBinaryTransformer(compressor))

/**
 * DB 저장 시 압축하고, 조회 시 복원합니다.
 */
class CompressedBinaryTransformer(
    private val compressor: Compressor,
): ColumnTransformer<ByteArray, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다 (압축).
     */
    override fun unwrap(value: ByteArray): ByteArray = try {
        compressor.compress(value)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to compress data (size: ${value.size} bytes)", e)
    }

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다 (복원).
     */
    override fun wrap(value: ByteArray): ByteArray = try {
        compressor.decompress(value)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to decompress data (size: ${value.size} bytes)", e)
    }
}
