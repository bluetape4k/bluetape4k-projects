package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toByteArray
import io.bluetape4k.support.toInt
import net.jpountz.lz4.LZ4Factory

/**
 * LZ4 알고리즘을 사용한 Compressor
 *
 * 참고: [lz4-java](https://github.com/lz4/lz4-java)
 *
 * ```
 * val compressor = LZ4Compressor()
 * val compressed = compressor.compress("Hello, LZ4!")
 * val decompressed = compressor.decompress(compressed)  // "Hello, LZ4!"
 * ```
 */
class LZ4Compressor: AbstractCompressor() {

    companion object: KLogging() {
        private const val MAGIC_NUMBER_SIZE: Int = Int.SIZE_BYTES

        private val factory: LZ4Factory by lazy { LZ4Factory.fastestInstance() }
        private val compressor by lazy { factory.fastCompressor() }
        private val decompressor by lazy { factory.fastDecompressor() }
    }

    override fun doCompress(plain: ByteArray): ByteArray {
        val sourceSize = plain.size
        val maxOutputSize = compressor.maxCompressedLength(sourceSize)

        val output = ByteArray(maxOutputSize + MAGIC_NUMBER_SIZE)
        sourceSize.toByteArray().copyInto(output, 0)

        val compressedSize = compressor.compress(plain, 0, sourceSize, output, MAGIC_NUMBER_SIZE, maxOutputSize)
        return output.copyOfRange(0, MAGIC_NUMBER_SIZE + compressedSize)
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        val sourceSize = compressed.toInt()
        val output = ByteArray(sourceSize)

        decompressor.decompress(compressed, MAGIC_NUMBER_SIZE, output, 0, sourceSize)
        return output
    }
}
