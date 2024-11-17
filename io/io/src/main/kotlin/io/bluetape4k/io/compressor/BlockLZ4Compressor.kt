package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import java.io.ByteArrayInputStream

/**
 * Apache Compress 라이브러리의 Block LZ4 알고리즘을 사용한 Compressor
 *
 * ```
 * val compressor = BlockLZ4Compressor()
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val plain = compressor.decompress(compressed)
 * ```
 *
 * @see [BlockLZ4CompressorInputStream]
 * @see [BlockLZ4CompressorOutputStream]
 */
class BlockLZ4Compressor: AbstractCompressor() {

    companion object: KLogging()

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        BlockLZ4CompressorOutputStream(output.outputStream()).use { lz4 ->
            lz4.write(plain)
            lz4.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        ByteArrayInputStream(compressed).use { input ->
            BlockLZ4CompressorInputStream(input).use { lz4 ->
                return Buffer().readFrom(lz4).readByteArray()
            }
        }
    }
}
