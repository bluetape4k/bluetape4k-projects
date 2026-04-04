package io.bluetape4k.io.compressor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/**
 * JDK Deflate 알고리즘을 이용한 압축기
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Deflate!".toByteArray()
 * val compressed = Compressors.Deflate.compress(data)
 * val restored = Compressors.Deflate.decompress(compressed)
 * // restored contentEquals data == true
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
        val output = ByteArrayOutputStream(plain.size)
        DeflaterOutputStream(output).use { deflate ->
            deflate.write(plain)
            deflate.finish()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            InflaterInputStream(input).use { inflate ->
                inflate.readBytes()
            }
        }
    }
}
