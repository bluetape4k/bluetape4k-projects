package io.bluetape4k.exposed.core.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table

/**
 * `ByteArray`를 압축해 `VARBINARY`에 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 [Compressor.compress], 조회 시 [Compressor.decompress]를 적용합니다.
 * - 변환 중 예외가 발생하면 [IllegalStateException]으로 감싸서 전파합니다.
 *
 * ```kotlin
 * val payload = table.compressedBinary("payload", 4096)
 * // payload.columnType.sqlType().contains("VARBINARY")
 * ```
 */
fun Table.compressedBinary(
    name: String,
    length: Int,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBinaryColumnType(compressor, length))

/** `VARBINARY` + 압축 변환기를 결합한 컬럼 타입입니다. */
class CompressedBinaryColumnType(
    compressor: Compressor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), CompressedBinaryTransformer(compressor))

/** 저장 시 압축하고 조회 시 복원하는 변환기입니다. */
class CompressedBinaryTransformer(
    private val compressor: Compressor,
): ColumnTransformer<ByteArray, ByteArray> {
    /** 엔티티 값을 DB 저장용 압축 바이트로 변환합니다. */
    override fun unwrap(value: ByteArray): ByteArray = try {
        compressor.compress(value)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to compress data (size: ${value.size} bytes)", e)
    }

    /** DB 바이트를 엔티티 값으로 복원합니다. */
    override fun wrap(value: ByteArray): ByteArray = try {
        compressor.decompress(value)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to decompress data (size: ${value.size} bytes)", e)
    }
}
