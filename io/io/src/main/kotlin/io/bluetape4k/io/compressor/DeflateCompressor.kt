package io.bluetape4k.io.compressor

import io.bluetape4k.io.okio.bufferOf
import okio.Buffer
import java.io.ByteArrayInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/**
 * JDK Deflate 알고리즘을 이용한 압축기
 *
 * ```
 * val compressor = DeflateCompressor()
 * val compressed = compressor.compress("Hello, Deflate!")
 * val decompressed = compressor.decompress(compressed)
 * ```
 *
 * @see [DeflaterOutputStream]
 * @see [InflaterInputStream]
 */
class DeflateCompressor: AbstractCompressor() {

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        DeflaterOutputStream(output.outputStream()).use { deflate ->
            deflate.write(plain)
            deflate.finish()
        }
        return output.readByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            InflaterInputStream(input).use { inflate ->
                bufferOf(inflate).readByteArray()
            }
        }
    }
}
