package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.StreamingCompressor
import io.bluetape4k.io.compressor.asCompressor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import java.io.IOException

/**
 * 데이터를 압축 해제하여 [okio.Source]로 읽는 [okio.ForwardingSource] 구현체.
 *
 * @see CompressableSink
 */
open class DecompressableSource(
    delegate: okio.Source,
    val compressor: Compressor,
): okio.ForwardingSource(delegate) {

    companion object: KLogging() {
        const val MAX_NO_PROGRESS_READS = 8
    }

    private val decodedBuffer = Buffer()
    private var decodedReady: Boolean = false

    /**
     * Okio 압축/해제에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        ensureDecoded()
        if (decodedBuffer.size == 0L) {
            return -1 // End of stream
        }

        val bytesToReturn = byteCount.coerceAtMost(decodedBuffer.size)
        sink.write(decodedBuffer, bytesToReturn)
        return bytesToReturn
    }

    private fun ensureDecoded() {
        if (decodedReady) {
            return
        }
        decodedReady = true

        val sourceBuffer = Buffer()
        var noProgressCount = 0
        while (true) {
            val bytesRead = super.read(sourceBuffer, Long.MAX_VALUE)
            if (bytesRead < 0L) {
                break
            }
            if (bytesRead == 0L) {
                noProgressCount++
                if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to read compressed bytes from source: no progress.")
                }
                continue
            }
            noProgressCount = 0
        }

        val sourceSize = sourceBuffer.size
        val decompressed = compressor.decompress(sourceBuffer.readByteArray())
        decodedBuffer.write(bufferOf(decompressed), decompressed.size.toLong())
        log.debug { "압축 복원: compressed=$sourceSize bytes, decompressed=${decodedBuffer.size} bytes" }
    }
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asDecompressSource` 함수를 제공합니다.
 */
fun okio.Source.asDecompressSource(compressor: Compressor): DecompressableSource {
    return DecompressableSource(this, compressor)
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asDecompressSource` 함수를 제공합니다.
 */
fun okio.Source.asDecompressSource(compressor: StreamingCompressor): DecompressableSource {
    return DecompressableSource(this, compressor.asCompressor())
}
